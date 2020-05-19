# actionne

一个基于规则的动作触发器框架


## Features

* 用简单的DSL定义条件和定义动作
* 用plugin定义输入数据集和动作函数
* 自定义配置项

## Usage

Check out this [dummy guide](doc/Installation-for-dummies.md) if you're new to the framework :)

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

可以参考examples目录的例子。

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

这里提供一个更复杂的脚本例子： [myscript.act](https://github.com/virushuo/actionne/blob/master/examples/scripts/myscript.act)

这个例子使用了4条触发删除动作的规则，分别为：

1. 删除 创建超过12小时 并且 like数量小于20 的全部回复
2. 删除 创建超过24小时 的全部retweet
3. 删除 创建超过24小时 并且 like数量小于20 并且 id不为"824653" （保留它的原因是我想保留我发过的第一条推） 并且不包含 #k 或者 #t 这两个标记的推
4. 和3类似，只是在96小时的时候改变一些参数

### 配置文件

配置文件用来定义运行周期，脚本名称，插件所需的环境变量和参数等。

一个配置文件的例子：

```
{ :actionne_twitter {
    :consumer-key ""
    :consumer-secret ""
    :user-token "" 
    :user-token-secret "" 
    :screen_name "YOURNAME"
    :search_term "from:YOURNAME"
    :watching "3 days"
    :first_tweet "2006-12-01"
    :lastest_tweet "2019-12-31"
    :backup true
    :dryloop false
 }
 :scripts{
    :myscript 20
 }

}
```
:actionne_twitter  是对应名称插件的配置项。配置项细节可以在 actionne_twitter 项目中看到。
:scripts 里面列出了脚本和对应脚本的运行周期。这里使用的配置 :myscript 前面保存的 myscript.act 的名字，20是每20秒运行一次。

## 运行

终于可以运行了。再次运行

    java -Dhomedir="/YOUR_PATH" -jar actionne-0.1.0-SNAPSHOT-standalone.jar

如果一切正常，会看到logs中提示已经正确加载了插件：

    INFO: loading... /YOUR_PATH/plugins/actionne_twitter.jar

这里我们使用了actionne_twitter插件，这个插件会先进行权限检查：

INFO: call actionne_twitter/core.startcheck for account: YOURNAME

如果没有登录信息，会提示oauth授权连接，允许访问之后把屏幕输出的pincode贴回终端回车，actionne_twitter会保存登录信息，之后进入正常运行状态。

此时在 /YOU_PATH/data 目录下面会出现用户数据，比如 YOUNAME-actionne_twitter-session.clj 用来记录插件所需的状态数据。 .backup.log 备份数据（如果在actionne_twitter配置中允许了备份）。 

只要actionne保持运行，它就会按照配置文件定义的运行周期反复获取数据，检查规则，执行动作。

Tips: 如果在脚本中设置一个永远满足的条件，对应的动作就会永远运行。利用这个办法，甚至可以删光自己全部历史twitter内容。（actionne_twitter插件提供了从web获取历史tweets的方式，不受twitter api的3200条tweets限制。

## License

Distributed under the Eclipse Public License.
