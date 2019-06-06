# sentinel-lib 基于注解的流控和降级配置库
## 目标背景

简化FlowRule和DegradeRule的配置,目前的配置方式是在需要降级的地方写上SentinelResource注解,
然后专门找个文件写FlowRule和DegradeRule,配置内容分散,resource名需要在多处拷贝,需要多加小心保持各处resource名的一致性,给代码维护增加负担.
使用注解定义FlowRule和DegradeRule后,资源名只需要出现一次, 限流和降级规则在定义SentinelResource处直接可见,降低配置分散度,维护起来比较轻松.

## 如何使用

1. 编译项目

2. 如果是Spring Boot用户,可以通过EnableSentinel注解开启包功能.注解生效时会自动注入Sentinel的SentinelResourceAspect类,所以不需要额外再配置.
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

如果是基于xml的spring配置,需要配置如下这个bean.同时SentinelResourceAspect也需要额外配置.
```xml
<bean class="com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect"></bean>
<bean class="com.ximalaya.business.Annotation.configuration.SentinelAnnotationBeanProcessor"></bean>
```

3. sentinel-lib库提供了多种流控配置注解和降级控制注解,如下
* FlowRuleDefine 默认直接拒绝行为的流控
* RateLimitFlowRuleDefine 限速流控
* WarmUpFlowRuleDefine 预热和预热限速流控
* DegradeRuleDefine 降级控制

4. 使用例子
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

4. 注解中的配置可以支持properties文件.例如application.properties包含如下内容
```ini
my.flow=123
my.degrade.count=500
my.degrade.tw = 21
```

则在注解中可以使用定义的变量,如下:
```java
    @WarmUpFlowRuleDefine(rateLimit = "30")
    @FlowRuleDefine(count = "#{getProperty('my.flow')}")
    @DegradeRuleDefine(count = "${my.degrade.count}", timeWindow = "${my.degrade.tw}")
    @SentinelResource(value="doSomething", blockHandler = "myBlockHandler", fallback = "myDegradeHandler")
    @RequestMapping("/doSomething")
    public Response doSomething() {
        return new Response("everything is ok");
    }
```
