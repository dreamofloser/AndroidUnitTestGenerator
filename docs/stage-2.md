# 第二阶段：规则化断言与异常场景

## 阶段目标

第二阶段在基础测试骨架上加入规则化测试场景，使生成结果从“能调用方法”提升到“具备基础断言”。

```text
方法模型 -> 测试场景 -> 输入样例 -> 断言生成 -> 异常验证 -> 报告统计
```

## 已完成内容

- 支持简单算术返回值断言。
- 支持简单 boolean 比较断言。
- 支持常见参数变体。
- 支持基础异常场景。
- 报告新增规则命中和 fallback 统计。

## 规则能力

### 简单算术返回值

支持识别：

```text
return a + b
return a - b
return a * b
return a / b
```

生成 `assertEquals` 断言。

### 简单 boolean 比较

支持识别：

```text
return age >= 18
return score >= 60
return count == 0
```

生成 true / false 两组场景。

### 参数变体

| 类型 | 变体 |
| --- | --- |
| `String` | `""`、`null` |
| `boolean` | `false` |
| `int` / `long` | `0`、`-1` |

### 异常场景

支持识别基础 `throw new IllegalArgumentException(...)`，并生成：

```java
@Test(expected = IllegalArgumentException.class)
```

## 报告增强

报告新增以下指标：

```text
Generated test methods
Generated assertions
Rule matched methods
Fallback methods
```

这些指标用于区分“规则化生成”和“保守兜底生成”。

## 阶段限制

- 不做完整符号执行。
- 不推断复杂对象状态。
- 不处理复杂分支、循环和跨方法语义。
- 字符串拼接和集合内容暂不做精确推断。

第二阶段的定位是引入可解释的确定性规则，而不是完整程序语义分析。