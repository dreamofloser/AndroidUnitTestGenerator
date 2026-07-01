# 第三阶段设计说明

## 阶段目标

第三阶段在规则化测试生成的基础上，增加对依赖对象的 Mock 支持，让框架可以处理常见的 Service、Repository、Manager 等业务类。

核心变化：

```text
构造函数依赖识别 -> Mockito mock 字段 -> setUp 初始化 -> when stub -> verify 验证
```

## 当前支持能力

### 1. 构造函数依赖识别

当构造函数参数不是基本类型、字符串或集合时，框架会把它视为依赖对象。

示例：

```java
public UserService(UserRepository repository) {
    this.repository = repository;
}
```

会生成：

```java
private UserRepository repository;
private UserService target;
```

### 2. Mockito 初始化

生成：

```java
@Before
public void setUp() {
    repository = mock(UserRepository.class);
    target = new UserService(repository);
}
```

### 3. 依赖调用 stub

对于：

```java
return repository.getUserName(id);
```

生成：

```java
when(repository.getUserName("sample")).thenReturn("sample");
```

### 4. 依赖调用验证

生成：

```java
verify(repository).getUserName("sample");
```

void 方法也会生成 verify：

```java
verify(repository).saveUserName("sample", "sample");
```

### 5. 报告增强

报告新增：

```text
Mocked dependencies
Mockito stubs
Mockito verifications
```

## 当前限制

- 暂时只支持构造函数注入形式的依赖。
- 暂时只识别简单的 `dependency.method(...)` 调用。
- 暂不处理链式调用，例如 `api.user().name()`。
- 暂不处理异步回调、协程、RxJava 等场景。
