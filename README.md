# Redis 笔记

## 1. NoSQL介绍


## 2. 基础使用

### 2.1. 性能测试
- 安装见 附录1.
```shell
# c 客户端数量， n 请求数量
redis-benchmark -h localhost -p 6379 -c 100 -n 100000
```

### 2.2. 数据类型

#### 2.2.1. String
```shell
strlen key
append key "str2"
getrange key 0 3 # 从0-第3位 等于substr

setex key 30 value  # 设置key value 同时设置30秒过期
setnx key value # 如果不存在就set

mset k1 v1 k2 v2 k3 v3  # 批量set
mget k1 k2 k3   # 批量get
msetnx  k1 v1 k2 v2 k3 v3  # 如果都不存在就 批量set ,具有原子性 要么全部成功 要么全部失败

getset key value # 先get 再set
```
- 对象存储
```shell
set user:1 {name:zhangsan,age:18} #json

mset user:1:name zhangsan user:1:age 18 # user:{id}:{attribute}
```

#### 2.2.2. Integer
```shell
incr key # 让数字自动加+
incrby key 10 # 让数字加+10
decr key # 让数字自动减+
decrby key 10 # 让数字加+10
```

#### 2.2.3. List
- 基础命令
```shell
Lpush list value
Rpush list value
Lrange list 0 # 第0位的数字
Lindex list value
Llen # 长度

Lpop list 
Rpop list
Lrem list 1 value # 精确地移除一个value

trim list 1 2 # 截断保留从第1到2位
```
- 复杂命令
```shell
RpopLpush list1 list2 # 将list1最后一个元素添加到list2左边

lset list 0 value # 让list第0位改成value,如果list 没有该index 就报错

Linsert list before "value" insertValue # 指定位置插入
```

#### 2.2.4. Hash
```shell
hset hash field value
hget hash field
hmset hash field1 value1 field2 value2 field3 value3
hget hash field1 field2 field3
hgetall hash # 获取全部
hlen hash # 长度
hexists hash field # 存在
hkeys hash # 键
hvals hash # 值

hdel hash field 

Hincrby hash field 步长 # 以步长自增
Hdecrby hash field 步长 # 以步长自减
Hsetnx hash field value # 不存在的话就set值
```
- 对象存储
```shell
hset user:1 name allen
hmset user:1 name allen age 24 height 171
```

#### 2.2.5. Set
```shell
sadd set "value" 

Smembers set # 查看所有值
Sismembers set value # 判断value是否在set中

scard set # 元素个数

Srandmember set (number) # 随机抽取 number个数的元素

Spop # 随机弹出

Smove set1 set2 "value" # 指定一个set1的值 移动到set2

# 交并全集
Sdiff set1 set2 
Sinter set1 set2
Sunion set1 set2
```

#### 2.2.6. Zset 有序集合
```shell
Zadd set "value" 

Zrange set 0 -1 # 查看所有值
Zrevrange set 0 -1 # 反转排序

Zcard set # 元素个数
Zcount set min max # min到max之间的元素个数


#Zrandmember set (number) # 随机抽取 number个数的元素
#
Zrem set key # 随机弹出
#
#Zmove set1 set2 "value" # 指定一个set1的值 移动到set2

```
- 有序集合独有
```shell
Zrangebyscore set min max # 从小到大排序
Zrangebyscore set -inf +inf withscores # 从小到大排序 并显示值
```

#### 2.2.6. Geospatial 地理位置
```shell
Geoadd china:city longitude latitude cityname # 经度 纬度 城市名，一次可添加多个，经纬度要在+-180以内

Geopos china:city cityname1 cityname2 # 获取经纬度，一次可添加多个

Geodis china:city cityname1 cityname2 # 获取直线距离

Georadius china:city longitude latitude radius km (withdist/withcoord) (count num)# 获取与目标经纬度 直线距离在radius km范围内的城市 (展示距离/坐标) (展示num个目标城市)
GeoradiusbyMember china:city beijing radius km (withdist/withcoord) (count num)# 获取与目标城市 直线距离在radius km范围内的城市 (展示距离/坐标) (展示num个目标城市)

GeoHash china:city beijing # 返回经纬度的hash值

```
- 通用
```shell
Zrange china:city 0 -1   # 查看所有
Zrem china:city beijing   # 移除
Zrange china:city 0 -1
```

#### 2.2.7. Hyperloglog 基数集
- 统计UV，相较于Set的统计，内存较小。我们要计数不是要存储用户ID。
- ！是容许误差的，官方误差：0.81%
```shell
PFadd users a b c d e 
PFcount users

PFmerge userGroupTotal userGroup1 userGroup2 # 将 1 和 2 合并到userGroupTotal
```

#### 2.2.8. Bitmap 位存储
- 统计，二进制 也是省内存
```shell
setbit sign 0 1 # 第0天 登录了，1-登录了，0-未登录
getbit sign 0   # 获取第0天是否登录
bitcount sign (start) (end) # 统计时间范围内 登录了多少天
```

### 2.3. 事务

#### 2.3.1. 事务
- 事务是一组命令的集合，会被序列化，顺序执行 
- 单条命令保证原子性，事务不保证
- redis没有事务隔离

```shell
multi # 开启事务
command 1 .... # 后面输入每个命令 都会进入Queue，等待被执行
command 2 .... 
# Discard # 放弃事务，队列中的都不执行
Exec # 执行
```

#### 2.3.2. 两种错误
- 编译异常-命令错误；运行时异常
- 编译异常所有的命令都不会执行，但如果是运行异常，其他的命令会照样执行！

#### 2.3.3. 乐观锁和悲观锁
- 悲观锁在所有操作都会上锁，乐观锁则根据操作和时机，并引入监控机制
```shell
watch money # 监控money 看他期间是否有被修改，如果有的话会让事务失败取消掉
muti
incr money # 还没执行的时候，money被另外一个线程修改了
exec
```
```shell
unwatch # 如果执行失败先解锁，再重复上面操作
```


## 3. Java 整合

### 3.1. Jedis


### 3.2. Springboot整合
- lettuce
#### 3.2.1. Springboot整合

#### 3.2.2. 自定义RedisTemplate

- 公司常用自己写的工具类，处理异常抛出和让方法别名和原装一致


## 4. redis配置

### 4.1. 网络
```text
bind 127.0.0.1
protect-mode yes
poet 6379
```

### 4.2. General 通用
```text
daemonize yes
pidfile /var/run/redis_6379.pid # 如果再后台运行就需要一个pidfile

日志
loglevel notice # debug > verbose > notice > warning
logfile "" # 日志路径

databases 16
```

### 4.3. 快照
```text
save # 在规定的时间内，操作了多少次之后，把数据持久化


rdbcompression yes      # 是否压缩RDB文件，需要消耗CPU资源
rdbchecksum yes         # 保存RDB文件时，是否进行校验
dbfilename dump.rdb     # RDB文件名
rdb-del-sync-files no   # 是否压缩RDB文件，需要消耗CPU资源

dir ./                  # RBD保存目录
```

### 4.4. Replicate 复制


### 4.5. Security 安全
```text
requirepass 123465 # 设置密码 
```
```shell
config set requirepass "123456" # 或者在cli中 
auth 123456 # 登录
```

### 4.6. Client 限制
```text
maxclients 10000 # 最大客户端
maxmemory <byte> # 最大内存量
maxmemory-policy noeviction # 内存达到上限后的操作 # 移除过期的keys，报错...
```
> 6种maxmemory-policy
> noeviction：当 Redis 实例达到 maxmemory 时返回错误。 不会覆盖或驱逐任何数据。  
> allkeys-lfu：在 Redis 4.0 或更高版本中可用。 从整个键集中逐出最不常用 (LFU) 键。  
> allkeys-lru：从整个键集中逐出最近最少使用的 (LRU) 键。  
> volatile-lfu：在 Redis 4.0 或更高版本中可用。 驱逐设置为 TTL（生存时间）到期的最不常用的密钥。  
> volatile-lru：驱逐设置为 TTL 到期的最近最少使用 (LRU) 密钥。  
> allkeys-random：从整个密钥空间中逐出随机密钥。  
> volatile-random：从具有 TTL 过期时间的集合中逐出随机密钥。  
> volatile-ttl：从 TTL 过期的集合中驱逐具有最短 TTL 的键。


## 5. 持久化

### 5.1. RDB
- 除了自己设置的操作次数条件以外，flush也会生成一个dump.rdb文件
- 默认配置已经够用，适合大规模数据恢复dump
- 对数据完整性要求不高，没触发下一次持久化之前的数据有可能会丢失
- 一般用于从机备用数据

### 5.2. AOF
- 默认不开启，默认是使用RDB模式进行持久化
```text
appendonly no
appendfilename "appendonly.aof"
appenddirname "appendonlydir"

# alway 每次操作都会消耗资源
# everysec 每秒同步一次，可能会丢失1s的数据
# no 让操作系统自己同步，速度最快
appendfsync everysec 

# 如果文件太大，就folk一个新的进程 将文件重写
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

```
- 优点：每次操作都同步，数据完整性更好
- 缺点：AOF文件比RDB更大，修复速度更慢，运行效率也更慢。
- appendoly.aof文件损坏的话无法重启redis，需要reids-check-aof --fix 对文件进行修复

### 5.3. 拓展
- 只做缓存的话，也不需要做任何持久化
- 如果RDB和AOF同时开启，那会优先载入AOF 因为它的数据更全
- RBD做从机备份的话15分钟保存一次就可以了，保留 save 900 1
- 如果不用AOF，用主从复制也可以提高可行性
- 主从同时宕机的话会损失几十分钟的数据，启动脚本也要比较主从机器的版本谁更新一点，微博就是这种架构


## 6. 消息订阅
```shell
Psubsribe pattern   # 订阅指定pattern的频道
subsribe channel    # 订阅频道

PUNsubsribe pattern # 退订指定pattern的频道
PUNsubsribe channel # 退阅频道

publish channel msg   # 发布

PUBSUB subcommand arg # 查看消息订阅系统状态
```
- 可以用来做实时消息系统 和 文章订阅，稍复杂的场景就需要用到消息队列MQ来做


## 7. 集群
- 作用：高可用、负载均衡
- 一般单台Redis服务器，占用内容不超20G，如果超过就要加机器
- 单台机器可用性太差，性能瓶颈明显
### 7.1. 集群环境搭建
```shell
info replication # 查看集群状态
```
1. 端口号 和 pidfile
2. log 名字
3. 备份dum.rdb 名字
```shell
slaveof 127.0.0.1:6379 # 暂时让这个服务作为6379的从机
```
- 配置文件中的replication 永久性修改端口的从机设置
```text
replicaof 127.0.0.1 6379 
```
### 7.2. 默认配置的问题
- 写只能由主机操作

- 主机宕机,从机依然能连接主机,但不能写
```text
# 如果主机断开连接,命令让其成为主机.主机恢复之后 还需要手动重新让其成为从机
slaveof no one # 哨兵模式可以自动进行此操作.
```

- 如果没有在配置中修改从机配置,从机在断开重连期间,数据不会自动更新过来从机
- 当使用命令更改其重新作为从机,主机就会全量同步


### 7.3. Sentinel 哨兵模式

#### 7.3.1. 介绍
- 如果主机宕机,会投票选取机器作为主机
- 哨兵会监控机器状态,经常会给哨兵配置一个哨兵,预防哨兵挂掉  
- 机制:
  - 当一个哨兵检测到服务器宕机,不会直接触发failover故障转移
  - 当后面的服务器也检测到该服务器宕机,并且达到一定数量时,就会发起投票 通过后进行failover 

#### 7.3.2. 开启
```shell
sentinel monitor myredis 127.0.0.1:6379 1 # 1 表示挂了之后投票选出新的主机

redis-sentinel sentinel.conf
```

#### 7.3.3. 配置
```text
port 26379

sentinel monitor myredis 127.0.0.1:6379 1 # 监控的主机对象

sentinel down-after-milliseconds mymaster 30000 # 默认宕机30秒后投票

sentinel notification-script mymaster /var/redis/notify.sh # 编写故障通知的脚本
```


### 7.4. 缓存穿透和雪崩 (面试常考)
#### 7.4.1. 缓存穿透
- 概念:缓存没有命中,就会去数据库查
- 布隆过滤器:在缓存之前加一个过滤器,过滤掉不存在的参数
- 缓存空对象:缓存miss之后返回的空对象也缓存起来,再访问就返回缓存中的空对象,保护后端数据源.会保存了大量的空对象的键,浪费内存

#### 7.4.2. 缓存雪崩
- 量太大,一下全部打在数据库上.特别是在缓存集体失效的时候
- 分布式锁:在缓存和数据库之间加锁,控制只能有一定量的线程在并行操作,其余在队列中等待
- redis高可用:异地多活
- 限流降级:Springcloud
- 数据预热:在正式部署的时候把可能的数据先预先访问一遍,加到服务器中,在即将发生大访问量的时候,手动触发加载可能用到的的Key


## 附录

### 1. 五步安装
1. 下载解压
```shell
wget https://github.com/redis/redis/archive/7.0-rc3.tar.gz
tar -zxvf 7.0-rc3.tar.gz
rm -r 7.0-rc3.tar.gz
```
2. 安装c和c++环境
```shell
apt-get install gcc
apt-get install g++
```
3. 安装redis
```shell
make
make install
```
4. 进入文件夹修改配置
```shell
cd redis-7.0-rc3
vim redis.conf # daemonize 改成 yes
```
5. 启动链接和脚手架
```shell
redis-server redis.conf
redis-cli
```

### 2. 常用命令
```shell
shutdown yes # 保存结束
exit # 推出脚手架程序
select 10 # 切换到第10个数据库
```

```shell
set key value
get key
key *

move key # 移除
flushall

exist name
expire name 10 # 让name 10秒过期，ttl name 可查看剩余时间

type key # 查看类型
```
