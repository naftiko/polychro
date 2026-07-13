from checkov.common.models.enums import CheckCategories, CheckResult
from checkov.yaml_doc.checks.base_yaml_check import BaseYamlCheck


class NoWildcardCorsCheck(BaseYamlCheck):
    def __init__(self):
        name = "Ensure exposes adapters do not allow wildcard CORS origin"
        id = "CKV_NAFTIKO_003"
        supported_resource_type = ["*"]
        categories = [CheckCategories.GENERAL_SECURITY]
        super().__init__(name=name, id=id, categories=categories,
                         supported_resource_type=supported_resource_type)

    def scan_resource_conf(self, conf):
        """Check that no CORS configuration uses '*' as origin."""
        return self._check_node(conf)

    def _check_node(self, node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key in ("cors", "corsOrigin", "cors-origin", "allowedOrigins"):
                    if self._is_wildcard(value):
                        return CheckResult.FAILED
                result = self._check_node(value)
                if result == CheckResult.FAILED:
                    return CheckResult.FAILED
        elif isinstance(node, list):
            for item in node:
                if isinstance(item, str) and item == "*":
                    return CheckResult.FAILED
                result = self._check_node(item)
                if result == CheckResult.FAILED:
                    return CheckResult.FAILED
        return CheckResult.PASSED

    def _is_wildcard(self, value):
        if isinstance(value, str) and value == "*":
            return True
        if isinstance(value, list) and "*" in value:
            return True
        return False


check = NoWildcardCorsCheck()
