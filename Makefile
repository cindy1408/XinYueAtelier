.PHONY: ci backend-ci frontend-ci

ci: backend-ci frontend-ci
	@echo "✅ All CI checks passed locally"

backend-ci:
	@echo "==> Backend: lint, build, test, coverage"
	cd atelier-backend && mvn checkstyle:check --batch-mode
	cd atelier-backend && mvn clean verify jacoco:report --batch-mode -Dspring.profiles.active=ci

frontend-ci:
	@echo "==> Frontend: lint, test, build"
	cd atelier-frontend && npm ci
	cd atelier-frontend && npm run lint
	cd atelier-frontend && npm test -- --coverage
	cd atelier-frontend && npm run build