package cn.fcr.domain.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.tngtech.archunit.lang.conditions.ArchConditions.haveRawReturnType;
import static com.tngtech.archunit.lang.conditions.ArchConditions.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@DisplayName("DDD 领域层架构边界自动化守卫测试")
public class DomainArchitectureGuardTest {

    private static JavaClasses domainClasses;

    @BeforeAll
    public static void setup() {
        // 导入当前 domain 模块下的所有生产代码，排除测试代码本身
        domainClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("cn.fcr.domain..");
    }

    @Test
    @DisplayName("守卫规则 1: 严禁在领域层出现以 Port 结尾的接口或类")
    public void domain_layer_should_not_have_port_naming() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain..")
                .should().haveSimpleNameNotEndingWith("Port")
                .because("根据最新六边形架构规范，外部系统交互契约应统一命名为 Gateway 并放置于 gateway 包下");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("守卫规则 2: 领域层 Repository 接口的方法严禁返回原生 Map")
    public void repository_methods_should_not_return_map() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..domain..adapter.repository..")
                .should(not(haveRawReturnType(Map.class)))
                .because("基础设施层数据必须通过防腐层(ACL)转换为强类型 Entity/VO，禁止使用 Map 破坏强类型契约");

        rule.check(domainClasses);
    }
}
