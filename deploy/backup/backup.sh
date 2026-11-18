#!/bin/sh
set -u
umask 022

PASTA=/backups
HORA=${BACKUP_HORA:-3}
DIAS=${BACKUP_DIAS:-14}
export PGPASSWORD="$POSTGRES_PASSWORD"

fazer_backup() {
  arquivo="$PASTA/financeiro-$1.dump"
  if pg_dump -h postgres -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f "$arquivo.parcial"; then
    mv "$arquivo.parcial" "$arquivo"
    echo "$(date '+%F %T') backup gravado: $arquivo"
    find "$PASTA" -name 'financeiro-*.dump' -mtime +"$DIAS" -delete
  else
    rm -f "$arquivo.parcial"
    echo "$(date '+%F %T') falha no backup" >&2
    return 1
  fi
}

if [ "${1:-}" = "agora" ]; then
  fazer_backup "manual-$(date +%F_%H%M%S)"
  exit $?
fi

mkdir -p "$PASTA"
echo "Backup diário a partir das ${HORA}h, guardando ${DIAS} dias em $PASTA"
while true; do
  sleep 600
  hoje=$(date +%F)
  hora_atual=$(date +%H)
  hora_atual=${hora_atual#0}
  if [ "$hora_atual" -ge "$HORA" ] && ! ls "$PASTA"/financeiro-"$hoje"_*.dump > /dev/null 2>&1; then
    fazer_backup "${hoje}_$(date +%H%M)"
  fi
done
