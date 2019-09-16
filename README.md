# actionne

一个基于规则的动作触发器框架


## Features

* 用简单的DSL定义条件和定义动作
* 用plugin定义输入数据集和动作函数
* 自定义配置项

## Usage

actionne 的目的是实现一个基于规则运行某个函数的框架。它可以解决很多问题，从删除符合一组自定义规则的信息，到控制智能家居设备，都可以基于它实现。

为了足够的灵活性和可扩充性，actionne使用了插件和DSL设计。用户可以使用DSL语言描述业务规则，当获得了符合规则的消息，actionne会直接调用插件中的函数，从而完成用户预期的动作。

actionne requires Clojure 1.9.0 or later.


Run actionne:

    java -Dhomedir="/YOUR_PATH" -jar actionne-0.1.0-SNAPSHOT-standalone.jar


## Set up actionne

如果是第一次运行，actionne会创建 /YOUR_PATH 目录，并因为无法找到配置文件退出。

目录结构如下：

/YOUR_PATH
          /config  配置文件
          /data    用户数据
          /plugins 插件
          /scripts DSL脚本

### 插件

* 目前只有 twitter 插件: [twitter_actionne](https://github.com/virushuo/actionne-twitter) 
* 复制 actionne_twitter-0.1.0-SNAPSHOT.jar 到 /YOUR_PATH/plugins 目录

### DSL脚本

在/YOUR_PATH/scripts 下建立规则脚本，如 myscript.act

一个最简单的脚本，必须包含以下文件头：

```
Ver 1.0.0
Namespace huoju/actionne_twitter
Desc testsnsrules
```

Namespaces: 任意的用户名字/插件名
Desc 任意描述字符

下面可以开始写自己的规则了:

```
Do delete created_at laterthan 12 hour category = str:reply
```

这条规则会调用插件中的 delete 函数，条件是: created_at早于12个小时，并且category是reply。其中 created_at/category等字段都是在插件的输入数据集中定义的，随后会详述。

规则条件可以任意组合，比如更复杂一点的：

```
Do delete created_at laterthan 12 hour category = str:reply favorite_count < 20 id != str:1171575426201862144 id != str:1171576952588853248 id != str:1171572788785795074

```
这条规则会调用插件中的 delete 函数，条件是: created_at早于12个小时，并且category是reply，并且favorite_count 小于 20，并且id 不是 1171575426201862144 并且id 不是 str:1171576952588853248 并且id 不是 str:1171572788785795074。

所有的条件之间都是 And （并且）的关系。

这里提供一个更复杂的脚本例子：

### 配置文件

配置文件用来定义运行周期，脚本名称，插件所需的环境变量和参数等。


## License

Distributed under the Eclipse Public License.
