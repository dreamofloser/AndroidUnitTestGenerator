# 第三阶段：依赖识别与 Mock 测试

## 阶段目标

第三阶段增加依赖对象识别和 Mock 支持，使框架能够处理 Service、Repository、Manager 等依赖外部对象的业务类。

```text
构造函数依赖识别 -> Mock 字段 -> 初始化目标对象 -> stub 依赖调用 -> verify 交互
```

## 已完成内容

- 识别构造函数中的依赖参数。
- Java 场景生成 Mockito mock。
- Kotlin suspend 场景生成 MockK `coEvery`、`coVerify`。
- 支持依赖调用 stub。
- 支持依赖调用 verify。
- 报告新增 mock 相关统计。

## 依赖识别规则

当构造函数参数不是基础类型、字符串或集合时，生成器将其视为依赖对象。

示例：

```java
public UserService(UserRepository repository) {
    this.repository = repository;
}
```

生成测试时会创建 mock 依赖，并使用该依赖构造目标对象。

## Java Mock 生成方式

典型生成结果：

```java
private UserRepository repository;
private UserService target;

@Before
public void setUp() {
    repository = mock(UserRepository.class);
    target = new UserService(repository);
}
```

依赖调用：

```java
when(repository.getUserName("sample")).thenReturn("sample");
verify(repository).getUserName("sample");
```

## Kotlin suspend Mock 生成方式

Kotlin 协程场景使用 MockK：

```kotlin
coEvery { api.getWeather(any(), any()) } returns WeatherResponse()
coVerify { api.getWeather(any(), any()) }
```

测试方法使用：

```kotlin
runTest { ... }
```

## 报告增强

报告新增：

```text
Mocked dependencies
Mockito stubs
Mockito verifications
```

其中字段名称仍沿用早期 Mockito 统计命名，但 Kotlin MockK 场景也会计入 mock stub 和 verification 数量。

## 阶段限制

- 主要支持构造函数注入。
- 主要识别简单 `dependency.method(...)` 调用。
- 不处理链式调用，例如 `api.user().name()`。
- 不完整支持异步回调、RxJava、复杂协程流。

第三阶段的作用是让生成器能够覆盖常见业务层依赖交互，而不是只测试无依赖工具类。