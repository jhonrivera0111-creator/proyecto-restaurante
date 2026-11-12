package com.casaverde.backend.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Prueba de contrato de arquitectura: verifica automaticamente que el codigo
 * respeta la separacion en capas definida en docs/arquitectura.md.
 *
 * Controller -> IXxxService -> XxxServiceImpl -> Repository -> Entity
 *
 * Si alguien rompe una regla (por ejemplo, un controller que llama a un repository),
 * esta prueba falla y el cambio no debe considerarse terminado.
 */
class ArquitecturaCapasTest {

    private static JavaClasses clases;

    @BeforeAll
    static void importarClases() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.casaverde.backend");
    }

    private static final String CONTROLADORES = "..capaControladores..";
    private static final String REPOSITORIOS = "..capaAccesoADatos.repositories..";
    private static final String ENTIDADES = "..capaAccesoADatos.models.entitys..";
    private static final String SERVICIOS = "..fachadaServices.services..";

    @Test
    void controladores_no_acceden_a_repositorios() {
        ArchRule regla =
            noClasses().that().resideInAPackage(CONTROLADORES)
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORIOS)
                    .because("los controllers solo delegan en la capa de servicios");
        regla.check(clases);
    }

    @Test
    void controladores_no_exponen_entidades() {
        ArchRule regla =
            noClasses().that().resideInAPackage(CONTROLADORES)
                    .should().dependOnClassesThat().resideInAPackage(ENTIDADES)
                    .because("la API devuelve DTOs, nunca entidades JPA");
        regla.check(clases);
    }

    @Test
    void controladores_dependen_de_interfaces_no_de_impl() {
        ArchRule regla =
            noClasses().that().resideInAPackage(CONTROLADORES)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("ServiceImpl")
                    .because("los servicios se inyectan por su interfaz IXxxService");
        regla.check(clases);
    }

    @Test
    void servicios_no_dependen_de_controladores() {
        ArchRule regla =
            noClasses().that().resideInAPackage(SERVICIOS)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLADORES)
                    .because("las dependencias van hacia abajo, nunca hacia arriba");
        regla.check(clases);
    }

    @Test
    void acceso_a_datos_no_depende_de_servicios_ni_controladores() {
        ArchRule regla =
            noClasses().that().resideInAPackage("..capaAccesoADatos..")
                    .should().dependOnClassesThat().resideInAnyPackage(SERVICIOS, CONTROLADORES)
                    .because("la capa de datos es la base: no conoce a las capas superiores");
        regla.check(clases);
    }

    @Test
    void repositorios_son_interfaces() {
        ArchRule regla =
            classes().that().resideInAPackage(REPOSITORIOS)
                    .should().beInterfaces()
                    .because("son interfaces Spring Data JPA");
        regla.check(clases);
    }

    @Test
    void sin_inyeccion_por_campo() {
        ArchRule regla =
            noFields().should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("se usa inyeccion por constructor (@RequiredArgsConstructor)");
        regla.check(clases);
    }

    @Test
    void repositorios_sin_anotacion_repository() {
        ArchRule regla =
            noClasses().that().resideInAPackage(REPOSITORIOS)
                    .should().beAnnotatedWith("org.springframework.stereotype.Repository")
                    .because("convencion del proyecto: Spring Data los registra solo");
        regla.check(clases);
    }
}
