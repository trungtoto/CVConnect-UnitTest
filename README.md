# CVConnect Unit Tests - Standalone Repository

Repo này chứa toàn bộ unit tests cho dự án CVConnect và có thể chạy độc lập mà không cần toàn bộ monorepo.

## Cấu trúc

```
core-tests/      - Unit tests cho core-service
gateway-tests/   - Unit tests cho api-gateway
user-tests/      - Unit tests cho user-service
scripts/         - Bootstrap scripts để clone dependency
```

## Yêu cầu

- **Java 17** (JDK)
- **Maven 3.8+**
- **Git**
- Quyền truy cập GitHub (để clone CVConnect main repo)

## Chạy Tests

### Cách 1: Tự động (PowerShell - Windows)

```powershell
# Từ thư mục gốc của repo này
.\scripts\test-bootstrap.ps1
```

Hoặc chỉ định branch khác:
```powershell
.\scripts\test-bootstrap.ps1 -Branch develop
```

### Cách 2: Tự động (Bash - Linux/Mac)

```bash
# Từ thư mục gốc của repo này
bash scripts/test-bootstrap.sh
```

Hoặc chỉ định branch khác:
```bash
bash scripts/test-bootstrap.sh https://github.com/trungtoto/CVConnect.git develop
```

### Cách 3: Manual

Nếu muốn chạy từng bước:

```bash
# 1. Clone CVConnect source
git clone --branch master https://github.com/trungtoto/CVConnect.git .cvconnect-source

# 2. Build các service chính (bỏ qua tests)
mvn -f .cvconnect-source/BE/pom.xml -pl core-service,api-gateway,user-service clean install -DskipTests=true

# 3. Chạy unit tests
mvn clean test
```

## Bootstrap Script là gì?

Script `test-bootstrap.ps1` / `test-bootstrap.sh` làm 3 việc:

1. **Clone source**: Clone CVConnect repo (nếu chưa có) vào thư mục `.cvconnect-source/`
2. **Build dependencies**: Compile core-service, api-gateway, user-service để test có thể import
3. **Run tests**: Chạy `mvn test` trên tất cả test modules

## Cấu hình tùy chỉnh

Nếu CVConnect source đã nằm ở một chỗ khác máy bạn, có thể set biến environment `CVCONNECT_SOURCE_DIR`:

```bash
export CVCONNECT_SOURCE_DIR=/path/to/your/CVConnect/BE
mvn -Dcvconnect.source.dir=$CVCONNECT_SOURCE_DIR clean test
```

Hoặc trực tiếp trong Maven:

```bash
mvn -Dcvconnect.source.dir=/path/to/CVConnect/BE clean test
```

## Troubleshooting

### 1. "Cannot find cvconnect" error

**Vấn đề**: Maven không tìm thấy các artifact từ core-service, api-gateway, user-service.

**Giải pháp**: 
- Đảm bảo đã chạy script bootstrap trước
- Hoặc build thủ công: `mvn -f .cvconnect-source/BE/pom.xml -pl core-service,api-gateway,user-service clean install -DskipTests=true`

### 2. "Jacoco report failed"

**Vấn đề**: Gateway tests không tìm thấy source code để report coverage.

**Giải pháp**:
- Đảm bảo `.cvconnect-source` đã được clone đúng
- Hoặc set `cvconnect.source.dir` trỏ đúng folder BE của CVConnect

### 3. "Permission denied" (Linux/Mac)

```bash
chmod +x scripts/test-bootstrap.sh
bash scripts/test-bootstrap.sh
```

## Commit và Push

Sau khi sửa file test, commit như bình thường:

```bash
git add core-tests/ gateway-tests/ user-tests/
git commit -m "Fix test XYZ"
git push
```

## CI/CD Integration

Để chạy tests tự động trong GitHub Actions:

```yaml
- name: Run CVConnect Tests
  run: |
    if [ -f scripts/test-bootstrap.sh ]; then
      bash scripts/test-bootstrap.sh
    else
      mvn clean test
    fi
```

---

**Note**: Lần chạy đầu tiên sẽ chậm hơn vì cần clone repo và build. Chạy sau sẽ nhanh hơn do reuse source/build cache.
