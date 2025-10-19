@echo off
echo Testing CourseFinder Backend Database Connection...
echo.

echo 1. Testing basic backend status...
curl -s http://localhost:8080/api/status
echo.
echo.

echo 2. Testing database health...
curl -s http://localhost:8080/api/database/health
echo.
echo.

echo 3. Creating test user...
curl -s -X POST http://localhost:8080/api/database/test-user
echo.
echo.

echo 4. Getting all users...
curl -s http://localhost:8080/api/database/users
echo.
echo.

echo 5. Deleting test user...
curl -s -X DELETE http://localhost:8080/api/database/test-user
echo.
echo.

echo Database connection test completed!
pause 