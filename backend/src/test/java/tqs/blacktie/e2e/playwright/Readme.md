 Para correr os testes E2E, o Docker NÃO deve estar a correr, porque os testes criam o seu próprio ambiente isolado.

 cd /home/pedro/Documents/3_ano/1_semestre/TQS/blacktie && docker compose down

 cd /home/pedro/Documents/3_ano/1_semestre/TQS/blacktie/frontend && npm run dev

 cd /home/pedro/Documents/3_ano/1_semestre/TQS/blacktie/backend && timeout 180 mvn test -Dtest="tqs.blacktie.e2e.playwright.*E2ETest"