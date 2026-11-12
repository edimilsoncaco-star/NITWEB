#!/data/data/com.termux/files/usr/bin/bash
set -e

# FILL: seus dados do GitHub
GH_USER="SEU_USUARIO"
GH_TOKEN="ghp_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
REPO_NAME="PainelNexus"

echo "=== Painel Nexus — Setup Termux ==="

# Permissão de storage (confirmar no popup)
termux-setup-storage
sleep 2

# Dependências
pkg update -y && pkg upgrade -y
pkg install git openjdk-17 -y

# Configurar git
git config --global user.name "${GH_USER}"
git config --global user.email "${GH_USER}@users.noreply.github.com"
git config --global credential.helper store
echo "https://${GH_USER}:${GH_TOKEN}@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials

REPO_URL="https://github.com/${GH_USER}/${REPO_NAME}.git"

# Cria repositório no GitHub se não existir
echo "=== Criando repositório no GitHub ==="
curl -s \
  -H "Authorization: token ${GH_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"${REPO_NAME}\",\"private\":true,\"description\":\"Painel Nexus ESP — Free Fire\"}" \
  https://api.github.com/user/repos > /dev/null

# Clona ou entra na pasta
if [ ! -d "${REPO_NAME}" ]; then
    git clone "${REPO_URL}" "${REPO_NAME}" 2>/dev/null || true
fi

# Se clone falhou (repo vazio), inicializa local
if [ ! -d "${REPO_NAME}/.git" ]; then
    mkdir -p "${REPO_NAME}"
fi

cd "${REPO_NAME}"

# Copia arquivos do projeto se veio por zip
# (ajuste o caminho se necessário)
if [ -d ~/storage/shared/PainelNexusFixed ]; then
    cp -r ~/storage/shared/PainelNexusFixed/. .
fi

# .gitignore
cat > .gitignore << 'GITIGNORE'
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

git init 2>/dev/null || true
git remote remove origin 2>/dev/null || true
git remote add origin "${REPO_URL}"
git branch -M main

git add .
git commit -m "feat: Painel Nexus v1 — ESP + VPN local + aimbot + BootReceiver" --allow-empty
git push -u origin main

echo ""
echo "=== Push feito com sucesso ==="
echo "=== Acompanhe o build APK em: ==="
echo "https://github.com/${GH_USER}/${REPO_NAME}/actions"
echo ""
echo "=== Quando o build terminar, baixe o APK em: ==="
echo "https://github.com/${GH_USER}/${REPO_NAME}/actions -> último workflow -> Artifacts"
