from checkov.common.models.enums import CheckCategories, CheckResult
from checkov.yaml_doc.checks.base_yaml_check import BaseYamlCheck


class EnforceHttpsBaseUriCheck(BaseYamlCheck):
    def __init__(self):
        name = "Ensure baseUri uses HTTPS scheme"
        id = "CKV_NAFTIKO_002"
        supported_resource_type = ["*"]
        categories = [CheckCategories.ENCRYPTION]
        super().__init__(name=name, id=id, categories=categories,
                         supported_resource_type=supported_resource_type)

    def scan_resource_conf(self, conf):
        """Check that all baseUri values use https://."""
        return self._check_node(conf)

    def _check_node(self, node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "baseUri" and isinstance(value, str):
                    if value.startswith("http://"):
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


check = EnforceHttpsBaseUriCheck()
