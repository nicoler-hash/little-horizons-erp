package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.util.ConsoleInput;

/** Main menu: groups the options by module, as in the project statement. */
public class MainMenu {

    private final StudentMenu studentMenu;
    private final AcademicMenu academicMenu;
    private final TeacherMenu teacherMenu;
    private final ExtracurricularMenu extracurricularMenu;
    private final FinanceMenu financeMenu;
    private final ResourceMenu resourceMenu;
    private final ReportMenu reportMenu;

    public MainMenu(StudentMenu studentMenu, AcademicMenu academicMenu, TeacherMenu teacherMenu,
                    ExtracurricularMenu extracurricularMenu, FinanceMenu financeMenu, ResourceMenu resourceMenu,
                    ReportMenu reportMenu) {
        this.studentMenu = studentMenu;
        this.academicMenu = academicMenu;
        this.teacherMenu = teacherMenu;
        this.extracurricularMenu = extracurricularMenu;
        this.financeMenu = financeMenu;
        this.resourceMenu = resourceMenu;
        this.reportMenu = reportMenu;
    }

    public void show() {
        int option;
        do {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════╗");
            System.out.println("║   ERP Colegio Little Horizons                ║");
            System.out.println("║   Sistema de Gestión Educativa               ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("1. Gestión de Estudiantes");
            System.out.println("2. Gestión Académica");
            System.out.println("3. Gestión de Profesores");
            System.out.println("4. Actividades Extracurriculares");
            System.out.println("5. Gestión Financiera");
            System.out.println("6. Gestión de Recursos");
            System.out.println("7. Reportes y Estadísticas");
            System.out.println("8. Salir");
            System.out.println();
            try {
                option = ConsoleInput.readInt("Seleccione una opción: ", 1, 8);
            } catch (OperationCancelledException e) {
                option = 8;
            }
            switch (option) {
                case 1 -> studentMenu.show();
                case 2 -> academicMenu.show();
                case 3 -> teacherMenu.show();
                case 4 -> extracurricularMenu.show();
                case 5 -> financeMenu.show();
                case 6 -> resourceMenu.show();
                case 7 -> reportMenu.show();
                default -> System.out.println("\nHasta pronto.");
            }
        } while (option != 8);
    }
}
