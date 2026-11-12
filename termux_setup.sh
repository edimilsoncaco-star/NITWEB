#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_NAME="PainelNexus"

echo "=== Painel Nexus — publicação segura ==="
pkg update -y
pkg install -y git gh openjdk-17

if ! gh auth status >/dev/null 2>&1; then
  echo "Faça login primeiro com: gh auth login"
  exit 1
fi

if [ ! -f "settings.gradle" ] || [ ! -d "app/src/main" ]; then
  echo "Execute este script na pasta raiz do projeto (onde está settings.gradle)."
  exit 1
fi

cat > .gitignore <<'GITIGNORE'
*.iml
.gradle/
local.properties
.idea/
app/build/
build/
captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
*.keystore
GITIGNORE

git init
git add .
if ! git diff --cached --quiet; then
  git -c user.name="Nexus Project" \
      -c user.email="nexus-project@users.noreply.github.com" \
      commit -m "chore: publish Painel Nexus utility panel"
fi

if ! gh repo view "$REPO_NAME" >/dev/null 2>&1; then
  gh repo create "$REPO_NAME" --private --source=. --remote=origin
else
  if ! git remote get-url origin >/dev/null 2>&1; then
    gh repo set-default "$REPO_NAME"
    git remote add origin "$(gh repo view "$REPO_NAME" --json url -q .url).git"
  fi
fi

git branch -M main
git push -u origin main

echo "Publicação concluída."
echo "Build do APK: abra a aba Actions do repositório no GitHub."
