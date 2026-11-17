#!/usr/bin/env bash
# Crea un historial de commits atómicos (mensajes en inglés) para el proyecto.
# Uso (desde la raíz del proyecto, con tu identidad de Git ya configurada):
#   git init -b main
#   git config user.name  "Tu Nombre"
#   git config user.email "tu.correo@ejemplo.com"
#   bash make_commits.sh
set -euo pipefail
B=src/main/java/com/littlehorizons/erp

commit() { # message, paths...
  local msg="$1"; shift
  git add -- "$@"
  git commit -q -m "$msg"
  echo "✓ $msg"
}

commit "chore: initialize Maven project with MySQL driver" pom.xml .gitignore LICENSE invoices/.gitkeep
commit "feat(util): add logging, console input and table printing utilities" \
  $B/exception $B/util/AppLogger.java $B/util/ConsoleInput.java $B/util/TablePrinter.java $B/util/Names.java
commit "feat(util): add JDBC template helper and database connection singleton" \
  $B/util/Jdbc.java $B/util/DatabaseConnection.java src/main/resources/db.properties
commit "feat(model): add domain entities" $B/model
commit "feat(dao): add DAO interfaces" $B/dao/*.java
commit "feat(dao): add JDBC implementations" $B/dao/impl
commit "feat(util): add simulated CUFE and text invoice generator" \
  $B/util/CufeGenerator.java $B/util/InvoiceTextGenerator.java \
  $B/util/SchoolConfig.java src/main/resources/school.properties
commit "feat(controller): add business rules for every module" $B/controller
commit "feat(view): add console menus for every module" $B/view
commit "feat: wire dependencies in Main" $B/Main.java
commit "docs: add database script with DDL and sample data" docs/database.sql
commit "docs: add README" README.md

if [ -n "$(git status --porcelain)" ]; then
  echo "⚠ Hay archivos sin confirmar:"; git status --short
else
  echo "Listo. Hash completo del último commit:"; git rev-parse HEAD
fi
