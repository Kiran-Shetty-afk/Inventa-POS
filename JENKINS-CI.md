# Jenkins CI for Inventa POS

## Existing CI audit

The repository had no Jenkinsfile, Jenkins directory, or Jenkins setup guide. It does have a separate GitHub Actions workflow at `.github/workflows/ci.yml`; that workflow checks the Java services, frontend, ML API, and Docker Compose configuration. The Jenkins pipeline below is a small demonstration pipeline for Java and frontend verification, and does not replace or duplicate GitHub Actions configuration.

Jenkins 2.568.2 is installed and active in the Ubuntu WSL distribution. Its controller environment currently reports Java 21 and has no Node.js executable. Jenkins is reachable from Windows at the current WSL distribution IP on port 8080 (HTTP 200); `localhost:8080` did not work from Windows during this check. Find the current address with `wsl.exe -d Ubuntu hostname -I` and open `http://<WSL-IP>:8080/`. The WSL IP can change after a restart. The controller needs a Node.js-capable agent or tool configuration before it can complete the frontend stage. No Jenkins job or build was created or run.

## Pipeline stages

The root `Jenkinsfile` has four stages:

1. **GitHub Checkout** — uses Jenkins `checkout scm` to check out the branch selected by the Jenkins job; repository credentials stay in Jenkins SCM configuration.
2. **Backend Build and Tests** — runs Maven Wrapper `verify` across all six Spring Boot modules and publishes Surefire JUnit XML reports.
3. **Frontend Build and Tests** — runs `npm ci` from the committed `package-lock.json`, then lint, Vitest, and the Vite production build.
4. **Pipeline Result** — reports successful completion. The pipeline's final `post` action reports success/failure even if an earlier stage fails, and successful builds archive frontend output and Java JARs.

The pipeline supports Unix and Windows Jenkins agents. On Unix, it invokes the checked-in Maven wrapper through `sh` because the wrapper is not marked executable in Git. It does not start Docker or change application configuration.

## Required Jenkins agent tools

Provision these tools on the Jenkins build agent (or configure the matching Jenkins tools and `PATH`):

- Git
- JDK 17 to match the Java version used by GitHub Actions; JDK 21 is also compatible and passed local Maven verification
- Node.js 22 and npm 11.6.2, matching the existing frontend CI setup
- No separate Maven installation is required; the repository includes Maven Wrapper files

The built-in Ubuntu WSL controller currently has Java 21 and no Node.js, so it needs a suitable agent/tool configuration before a pipeline build can pass. This project setup did not install tools or modify Jenkins machine configuration. The job needs Jenkins Pipeline, Git, and JUnit support; confirm those plugins are available in the Jenkins installation before creating the job.

## Configure the Jenkins job

1. In Windows PowerShell, run `wsl.exe -d Ubuntu hostname -I`, then open `http://<WSL-IP>:8080/` using the current Ubuntu WSL address. The Jenkins listener returned HTTP 200 at that address during this check.
2. Create a **Pipeline** job. For branch selection and `checkout scm`, select **Pipeline script from SCM** and choose **Git**.
3. Enter the existing GitHub repository URL from the repository's Git configuration. Do not add a token to the URL or Jenkinsfile. For a private repository, select a Jenkins-managed credential.
4. Set the branch specifier to the branch you want to build (for example, `*/main`) or use a Multibranch Pipeline to discover branches automatically.
5. Set the script path to `Jenkinsfile`, save, then select **Build Now**.

`checkout scm` uses the SCM configuration and selected branch from that job; it does not contain repository credentials. The job should run on an agent with the tools listed above.

## Run and review a build

- Select **Build Now** for a manual build.
- Open the new build's **Console Output** to review checkout, Maven, lint, test, and production-build steps.
- Open **Test Result** to review published Java JUnit reports. Frontend Vitest results are shown in Console Output; the pipeline does not add a separate frontend test-report plugin.
- On success, open **Build Artifacts** to download the Vite `dist` output and service JARs.

## Validation performed

Local equivalents passed on Windows with Java 21, Node.js 24, and npm 11.6.2: Maven Wrapper `verify` (70 tests), frontend ESLint, Vitest (15 tests), and Vite production build. The production build emitted Vite's existing large-chunk advisory but completed successfully. `npm ci` is configured in the Jenkins pipeline to use the lockfile; it was not run locally so the current `node_modules` installation would not be replaced.

An actual Jenkins job was not run. The Jenkins Declarative linter endpoint required authenticated Jenkins read access and returned HTTP 403 for the anonymous request; no Jenkins credentials were accessed. Review the Jenkinsfile in the job or validate it from an authenticated Jenkins session before the first demonstration build.

## Optional GitHub webhook

For automatic builds later, configure a webhook in the GitHub repository to the Jenkins endpoint exposed by your Jenkins installation, and enable the matching GitHub hook trigger in the Jenkins job. This setup intentionally does not create a webhook or expose Jenkins to the public internet.
