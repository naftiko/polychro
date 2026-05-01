from checkov.common.models.enums import CheckCategories, CheckResult
from checkov.yaml_doc.checks.base_yaml_check import BaseYamlCheck


class NoHardcodedSecretsCheck(BaseYamlCheck):
    def __init__(self):
        name = "Ensure no hardcoded secrets in authentication blocks"
        id = "CKV_NAFTIKO_001"
        supported_resource_type = ["*"]
        categories = [CheckCategories.ENCRYPTION]
        super().__init__(name=name, id=id, categories=categories,
                         supported_resource_type=supported_resource_type)

    def scan_resource_conf(self, conf):
        """Look for literal tokens/passwords in authentication blocks."""
        return self._check_node(conf)

    def _check_node(self, node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "authentication":
                    if self._has_hardcoded_secret(value):
                        return CheckResult.FAILED
                result = self._check_node(value)
                if result == CheckResult.FAILED:
                    return CheckResult.FAILED
        elif isinstance(node, list):
            for item in node:
                result = self._check_node(item)
                if result == CheckResult.FAILED:
                    return CheckResult.FAILED
        return CheckResult.PASSED

    def _has_hardcoded_secret(self, auth_node):
        if not isinstance(auth_node, dict):
            return False
        secret_keys = {"token", "password", "secret", "apiKey", "api-key", "api_key"}
        for key, value in auth_node.items():
            if key in secret_keys and isinstance(value, str):
                if not value.startswith("${") and not value.startswith("{{"):
                    return True
        return False


check = NoHardcodedSecretsCheck()
