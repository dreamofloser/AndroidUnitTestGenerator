# 第二阶段设计说明

## 阶段目标

第二阶段在第一阶段“可生成测试骨架”的基础上，引入规则化测试场景，让生成结果更接近真实单元测试。

核心变化：

```text
方法模型 -> 测试场景 -> 测试方法 -> 断言/异常验证 -> 增强报告
```

## 当前支持规则

### 1. 简单算术返回值

支持识别：

```text
return a + b
return a - b
return a * b
return a / b
```

生成 `assertEquals`。

### 2. 简单 boolean 比较

支持识别：

```text
return age >= 18
return score >= 60
return count == 0
```

生成 true/false 两组场景，例如边界值和边界值前一位。

### 3. 参数变体

对常见参数生成额外输入：

```text
String: "", null
boolean: false
int/long: 0, -1
```

### 4. 异常场景

识别方法中的简单：

```java
throw new IllegalArgumentException(...)
```

生成 JUnit4 的：

```java
@Test(expected = IllegalArgumentException.class)
```

### 5. 报告增强

报告新增：

```text
Generated test methods
Generated assertions
Rule matched methods
Fallback methods
```

## 当前限制

- 只做轻量静态规则，不做完整符号执行。
- 复杂对象状态、复杂分支和跨方法调用暂时回退到安全骨架。
- 字符串拼接和集合内容暂不做精确结果推断。
- Kotlin 源码解析仍属于后续阶段。
