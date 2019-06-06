# sentinel-lib 使用注解配置配置熔断限流规则
## 背景和目的

- Sentinel的限流降级配置需要手动编码实现,定义限流熔断规则的地方往往和使用SentinelResource注解的代码不在同一个文件,配置分散不方便管理.
- ResourceName需要在SentinelResource中,降级的地方,限流的地方多处重复定义,可能会出现手误导致出错,

为解决这些问题,sentinel-lib提供了一些注解,可以直接在使用SentinelResource注解的地方直接配置熔断限流规则.

## 如何使用

### 1. 编译项目

### 2. SpringBoot配置
如果是Spring Boot用户,可以通过`@EnableSentinel`注解开启包功能.注解生效时会自动注入Sentinel的`SentinelResourceAspect`类,所以**不需要**额外再配置.
例:
```java
@EnableSentinel
@SpringBootApplication
public class Starter extends SpringBootServletInitializer {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Starter.class, args);
    }
}
```

### 3. Spring的xml配置
如果是基于xml的spring配置,需要配置如下这个两个bean.
```xml
<bean class="com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect"></bean>
<bean class="com.ximalaya.business.Annotation.configuration.SentinelAnnotationBeanProcessor"></bean>
```

### 4. 可用的注解类简介 
sentinel-lib库提供了多种流控配置注解和降级控制注解,如下

| 注解类 | 功能 |
| -------- | ------- |
|FlowRuleDefine |默认直接拒绝行为的流控|
|RateLimitFlowRuleDefine| 限速流控|
|WarmUpFlowRuleDefine |预热和预热限速流控|
|DegradeRuleDefine |降级控制|

### 5. 使用例子
```java
@RestController
@RequestMapping("sample")
public class MyFlowSampleController {
    
    public String fallback() {
      return "fallback";
    }
    
    public String blockHandler(BlockException ex) {
      return "blocked!";
    }
    
    @WarmUpFlowRuleDefine
    @FlowRuleDefine(count = "500")
    @DegradeRuleDefine(count = "500", timeWindow = "10")
    @SentinelResource(value="doSomething", blockHandler = "blockHandler", fallback = "fallback")
    @RequestMapping("/doSomething")
    public String doSomething() {
        return new String("everything is ok");
    }

    @RateLimitFlowRuleDefine
    @FlowRuleDefine
    @DegradeRuleDefine
    @SentinelResource(value="exceptionApi", blockHandler = "blockHandler", fallback = "fallback")
    @RequestMapping("/exceptionApi")
    public Response exceptionApi() {
        throw new RuntimeException("something is wrong!");
    }
}
```

### 6. 使用properties
注解中的配置可以支持properties文件.例如application.properties包含如下内容
```ini
my.flow=123
my.degrade.count=500
my.degrade.tw = 21
```

则在注解中可以使用定义的变量,如下:
```java
@WarmUpFlowRuleDefine(rateLimit = "30")
@FlowRuleDefine(count = "${my.flow}")
@DegradeRuleDefine(count = "${my.degrade.count}", timeWindow = "${my.degrade.tw}")
@SentinelResource(value="doSomething", blockHandler = "myBlockHandler", fallback = "myDegradeHandler")
@RequestMapping("/doSomething")
public Response doSomething() {
    return new Response("everything is ok");
}
```

### 7. 关于灵活性
在注解中使用properties文件属性,可以支持在dev,test,prd环境使用不同的限流数量配置.

如果需要更加灵活的在线实时修改配置能力,建议使用ASAH的Sentinel控制台来做.
