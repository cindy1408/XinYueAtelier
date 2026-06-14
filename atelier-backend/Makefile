ci:
	mvn clean package -Dspring.profiles.active=ci
	cd atelier-frontend && npm ci && npm test && npm run lint

lint: 
	cd atelier-frontend && npx eslint . --fix

test:
	mvn clean test jacoco:report