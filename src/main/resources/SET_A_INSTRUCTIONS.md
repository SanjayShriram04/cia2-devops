# Set A – Step-by-Step Instructions

**Before you start, note these four things:**
- Run every command from the inner project folder, the one that contains `pom.xml`:
  `C:\Users\snucse\Downloads\CIA_2_Evaluation_IOT_B\Campus-Equipment-Booking-Student-Starter\Campus-Equipment-Booking-Student-Starter`
- `C:\Users\snucse\.git` is a broken file, and git currently fails with "invalid gitfile format". Running `git init` inside the project folder gives the project its own repo, which fixes this for the project.
- **The bug:** line 13 of `BookingService.java` reads `if(quantity > e.getAvailableQuantity() + 1)`. The `+ 1` lets a booking go 1 unit over the stock.
- **A conflict to know about:** the existing test `cancellationRestoresInventoryOnce` books **3** tablets. Once the max-2 rule is in, that test will fail.

---

## Task 1 – Git setup
```powershell
cd "C:\Users\snucse\Downloads\CIA_2_Evaluation_IOT_B\Campus-Equipment-Booking-Student-Starter\Campus-Equipment-Booking-Student-Starter"
git init
git add .
git commit -m "Initial commit: supplied baseline"
git checkout -b feature/booking-validation
```
Commit separately as you go (see each task below). At the end, run:
```powershell
git branch
git log --oneline --graph --all
git status
```
Save these outputs for the README.

## Task 2 – Maven build on the untouched baseline
1. Open `pom.xml` and write down:
   - Java 17 and Spring Boot parent 3.3.5
   - Dependencies: web, actuator, validation, and test (JUnit 5)
   - Plugin: `spring-boot-maven-plugin`
   - Folders: `src/main/java`, `src/main/resources`, `src/test/java`
2. Run `mvn clean test` and record the Tests run / Failures / Errors / Skipped counts. You should see 6 tests passing. The `+1` bug isn't caught because no existing test checks it.
3. Run `mvn package`, then `dir target`. You should see `campus-equipment-booking-1.0.0.jar`.
4. Copy the real output into a notes file for the README.

## Task 3 – Fix the capacity bug
1. In `BookingService.java` line 13, change `quantity > e.getAvailableQuantity() + 1` to `quantity > e.getAvailableQuantity()`.
2. Run `mvn test` and commit **only this file**:
   `git commit -am "Fix off-by-one in booking capacity validation"`
3. Add these tests to `BookingServiceTest.java` (keep all existing tests):
   - **Exceeds stock:** first bring the stock down so you stay under the max-2 rule later. For example, book `PROJECTOR01` 2 units, then 1 unit, leaving 1. Then try 2 units: expect `IllegalStateException`, stock still 1, and the earlier booking count unchanged.
   - **Exactly matches stock:** with 2 projectors left, book 2. It should succeed with status `CONFIRMED` (check `Booking.java` for the actual status value) and stock should be 0.
   - **Valid booking accepted:** check the returned booking's id, quantity and status.
   - **Rejected booking:** stock is unchanged. You can only see bookings by id, so asserting the exception plus unchanged stock is enough.
4. Run `mvn clean test`; everything should pass. Commit:
   `git commit -am "Add capacity boundary tests"`

## Task 4 – Max 2 units per booking
1. Add the rule in `BookingService.create()`, the service layer:
   ```java
   private static final int MAX_QUANTITY_PER_BOOKING = 2;
   ...
   if(quantity > MAX_QUANTITY_PER_BOOKING) throw new IllegalArgumentException("quantity must not exceed 2 per booking");
   ```
   **Put it after the capacity check.** The existing test `ordinaryOverbookingRejected` books 6 projectors and expects `IllegalStateException`. If the max check runs first, it throws `IllegalArgumentException` and that test fails.
2. Add tests:
   - 2 laptops: accepted, stock goes from 10 to 8.
   - 3 laptops: `IllegalArgumentException`, stock stays 10.
3. Run `mvn clean test`. `cancellationRestoresInventoryOnce` will fail because it books 3 tablets, which the new rule now rejects.
   - Change its quantity from 3 to 2 and its expected value to 8. The test still checks the same thing (cancelling restores stock once).
   - Explain this change in the README so it doesn't look like you weakened a test.
4. Commit:
   `git commit -am "Limit single booking to 2 units (Set A change request)"`

## Task 5 – Docker
1. Create `Dockerfile` in the project root:
   ```dockerfile
   FROM eclipse-temurin:17-jre
   WORKDIR /app
   COPY target/campus-equipment-booking-1.0.0.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java","-jar","app.jar"]
   ```
2. Build and run:
   ```powershell
   mvn clean package
   docker build -t campus-equipment-booking:set-a .
   docker run -d --name ceb -p 8080:8080 campus-equipment-booking:set-a
   docker ps
   curl.exe http://localhost:8080/actuator/health
   curl.exe http://localhost:8080/api/equipment
   ```
3. Optionally, test a booking:
   `curl.exe -X POST http://localhost:8080/api/bookings -H "Content-Type: application/json" -d "{\"borrowerId\":\"STU1\",\"equipmentId\":\"LAPTOP01\",\"quantity\":3}"`
   This should return **400** because of the max-2 rule.
4. Save the outputs, then stop and remove the container:
   `docker stop ceb; docker rm ceb`
5. Commit: `git add Dockerfile; git commit -m "Add Dockerfile"`

## Task 6 – Minikube
1. Create `deployment.yaml`:
   - `replicas: 2`
   - label `app: campus-equipment-booking` on both the selector and the pod template
   - image `campus-equipment-booking:set-a`
   - **`imagePullPolicy: Never`**
   - `containerPort: 8080`
2. Create `service.yaml`:
   - `type: NodePort`
   - selector `app: campus-equipment-booking`
   - `port: 8080`, `targetPort: 8080`
   - `nodePort: 30080` (optional)
3. Run:
   ```powershell
   minikube start
   minikube image load campus-equipment-booking:set-a
   kubectl apply -f deployment.yaml
   kubectl apply -f service.yaml
   kubectl get deployments,pods,svc -o wide
   minikube service campus-equipment-booking --url
   ```
   On Windows, keep the `minikube service` terminal open. Then run `curl.exe <url>/actuator/health`.
4. Scale up and test self-healing:
   ```powershell
   kubectl scale deployment campus-equipment-booking --replicas=3
   kubectl get pods
   kubectl delete pod <one-pod-name>
   kubectl get pods -w     # a new pod appears; press Ctrl+C
   kubectl get deployment campus-equipment-booking
   ```
5. For the README, explain self-healing: the Deployment manages a ReplicaSet. Its controller keeps comparing the desired replica count with the running pods and creates or removes pods to match.
6. Commit: `git add deployment.yaml service.yaml; git commit -m "Add Kubernetes manifests"`

## Task 7 – README
Replace `README.md` with these sections, filled in with your **real** outputs:
1. Setup and run commands
2. Git evidence (branch name, `git log`, summary of changes)
3. Maven evidence (test counts before and after, package result)
4. Docker evidence (Dockerfile, image tag, commands, health/API responses)
5. Kubernetes evidence (both YAML files, kubectl outputs, scaling and pod replacement)
6. Changes and limitations:
   - the `+1` bug fix
   - the max-2 rule
   - files changed
   - why the cancellation test now uses 2 units
   - stock is in memory and resets when the app restarts

Then commit: `git commit -am "Add submission README"`. Finally, run `git status` and check that it shows a clean working tree and that `target/` isn't committed.
