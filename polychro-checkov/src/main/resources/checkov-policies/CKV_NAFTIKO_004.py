from checkov.common.models.enums import CheckCategories, CheckResult
from checkov.yaml_doc.checks.base_yaml_check import BaseYamlCheck


class ExternalReferenceForSecretsCheck(BaseYamlCheck):
    def __init__(self):
        name = "Ensure secrets use external references (env vars or vault)"
        id = "CKV_NAFTIKO_004"
        supported_resource_type = ["*"]
        categories = [CheckCategories.ENCRYPTION]
        super().__init__(name=name, id=id, categories=categories,
                         supported_resource_type=supported_resource_type)

    def scan_resource_conf(self, conf):
        """Check that secret-like values use external references."""
        return self._check_node(conf, in_sensitive_context=False)

    def _check_node(self, node, in_sensitive_context):
        if isinstance(node, dict):
            for key, value in node.items():
                sensitive = in_sensitive_context or self._is_sensitive_key(key)
                if sensitive and isinstance(value, str):
                    if not self._is_external_ref(value):
                        return CheckResult.FAILED
                elif isinstance(value, (dict, list)):
                    result = self._check_node(value, sensitive)
                    if result == CheckResult.FAILED:
                        return CheckResult.FAILED
        elif isinstance(node, list):
            for item in node:
                result = self._check_node(item, in_sensitive_context)
                if result == CheckResult.FAILED:
                    return CheckResult.FAILED
        return CheckResult.PASSED

    def _is_sensitive_key(self, key):
        sensitive_keys = {"secret", "password", "token", "apiKey", "api-key",
                          "api_key", "clientSecret", "client-secret"}
        return key in sensitive_keys

    def _is_external_ref(self, value):
        return (value.startswith("${") or value.startswith("{{")
                or value.startswith("vault:") or value.startswith("env:"))


check = ExternalReferenceForSecretsCheck()
