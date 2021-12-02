# so-simple-test-springboot-starter

## 一个用于springboot的测试工具

简介：在springboot环境，特别是web项目，开发者经常要用postman对controller接口进行测试。这种方式仅能测试controller接口，且需要安装postman软件，还不能用于测试RPC服务（dubbo）。
    对于RPC服务能不能有一个类似postman的方式进行调用？本工具就是通过内置web提供类似postman一样的测试页面，而且能够自动分析出入参结构，无需费神构造入参。实际上，该工具可以任意指定bean的方法直接进行调用。

​	需要注意的是，该工具仅适用于开发测试环境，在线上环境部署会导致巨大隐患，因为可以通过页面调用任意方法。



**环境**：springboot 工程 和 java8



**Step1:** 添加依赖

```
<dependency>
   <groupId>io.github.ll-sanmu-ll</groupId>
   <artifactId>so-simple-test-springboot-starter</artifactId>
   <version>${version}</version>
</dependency>
```

**Step2：**spring properties 配置示例 `application.yml`:

```
server:
  port: 8080
```

**Step3:** 使用 @EnableServiceTesting 注解

```
@EnableServiceTesting({"controller","service","dao"})
@Configuration
public class TestConfig {

}
```

​	示例中的注释会把所有名称中带有`"controller","service","dao"`的bean添加到测试页面

**Step4:** 使用浏览器打开测试页面 http://localhost:8080/testing/index 

