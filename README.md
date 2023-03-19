# LightSwallow

LightSwallow 是一个基于Kotlin语言，运行在Linux环境上的轻量级程序运行沙盒。  
它可为程序运行任务提供基本的资源隔离、限制与控制能力。

它可以适用于如下场合：  

1. **程序在线评测系统 (OnlineJudge)**：可为此类OJ系统提供程序程序隔离运行功能
2. **简单的资源控制场景**：可为临时需要做资源限制的场景提供比简易的资源控制功能
3. **代码校验系统**：可为外来且需执行的代码提供隔离化的运行空间

## 运行环境需求

LightSwallow对运行环境要求：

1. Linux系统平台，64位操作系统 (如 Ubuntu Server 22.04)
2. 使用cgroup v1，且开启了`memory and swap accounting`
3. 安装了Java 17及以上

## 构建

构建环境需要安装`cmake` 3.10或以上版本，同时须有Java 17或以上版本运行环境。  

将项目代码保存至本地，切换终端至项目源码根目录下，构建方法参见`build.sh`文件，可执行

```sh
sh build.sh
```

执行后，可在项目代码目录下生成的`product`目录内获得构建好的jar文件和so文件。

## 感谢

此项目的开发离不开这些支持

* [simple-sandbox](https://github.com/Menci/simple-sandbox): 基础架构等基于此项目
* [QUDOJ-Judger](https://github.com/QingdaoU/Judger): 基础架构等基于此项目
