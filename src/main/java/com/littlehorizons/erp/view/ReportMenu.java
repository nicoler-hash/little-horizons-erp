package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;

/** Console menu for reports and statistics. */
public class ReportMenu {

    private final ReportController reports;
    private final FinanceController finance;
    private final AcademicController academic;
    private final ResourceController resources;

    public ReportMenu(ReportController reports, FinanceController finance, AcademicController academic,
                      ResourceController resources) {
        this.reports = reports;
        this.finance = finance;
        this.academic = academic;
        this.resources = resources;
    }

    public void show() {
        Menu.run("Reportes y estadísticas", List.of(
                new Menu.Item("Estudiantes activos por grado", this::studentsPerGrade),
                new Menu.Item("Promedio del curso por materia", this::courseAverages),
                new Menu.Item("Estudiantes que necesitan apoyo (promedio < 3.0)", this::studentsNeedingSupport),
                new Menu.Item("Inasistencias preocupantes", () -> TablePrinter.print(
                        academic.absenceAlerts(ConsoleInput.readInt("Semanas hacia atrás (1-52): ", 1, 52)))),
                new Menu.Item("Estudiantes morosos", () -> TablePrinter.print(finance.delinquentStudents())),
                new Menu.Item("Recaudo por concepto", () -> TablePrinter.print(finance.revenueByConcept(
                        ConsoleInput.readInt("Año: ", 2000, 2100)))),
                new Menu.Item("Participación en clubes", () -> TablePrinter.print(reports.clubParticipation())),
                new Menu.Item("Participación en eventos", () -> TablePrinter.print(reports.eventParticipation())),
                new Menu.Item("Préstamos vencidos", () -> TablePrinter.print(resources.overdueReport()))));
    }

    private void studentsPerGrade() {
        int year = ConsoleInput.readInt("Año académico (" + LocalDate.now().getYear() + "): ", 2000, 2100);
        TablePrinter.print(reports.studentsPerGrade(year));
    }

    private void courseAverages() {
        List<IdName> grades = reports.gradeLevels();
        grades.forEach(g -> System.out.printf("  %2d. %s%n", grades.indexOf(g), g.name()));
        int level = ConsoleInput.readInt("Grado (0-" + (grades.size() - 1) + "): ", 0, grades.size() - 1);
        int year = ConsoleInput.readInt("Año académico: ", 2000, 2100);
        String period = ConsoleInput.readChoice("Período:", AcademicController.GRADE_PERIODS.toArray(new String[0]));
        TablePrinter.print(reports.courseAverages(grades.get(level).id(), year, period));
    }

    private void studentsNeedingSupport() {
        int year = ConsoleInput.readInt("Año académico: ", 2000, 2100);
        String period = ConsoleInput.readChoice("Período:", AcademicController.GRADE_PERIODS.toArray(new String[0]));
        TablePrinter.print(reports.studentsNeedingSupport(year, period));
    }
}
