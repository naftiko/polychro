from checkov.common.models.enums import CheckCategories, CheckResult
from checkov.yaml_doc.checks.base_yaml_check import BaseYamlCheck
import re


class NoEvalInDescriptionsCheck(BaseYamlCheck):
    def __init__(self):
        name = "Ensure no eval-like expressions in description fields"
        id = "CKV_NAFTIKO_005"
        supported_resource_type = ["*"]
        categories = [CheckCategories.GENERAL_SECURITY]
        super().__init__(name=name, id=id, categories=categories,
                         supported_resource_type=supported_resource_type)

    _EVAL_PATTERN = re.compile(
        r"(eval\s*\(|exec\s*\(|Function\s*\(|setTimeout\s*\(|setInterval\s*\()",
        re.IGNORECASE
    )

    def scan_resource_conf(self, conf):
        """Check that description fields do not contain eval-like expressions."""
        return self._check_node(conf)

    def _check_node(self, node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "description" and isinstance(value, str):
                    if self._EVAL_PATTERN.search(value):
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


check = NoEvalInDescriptionsCheck()
