## 编译AOSP





### 环境搭建

参考文献

> [source.android.com:为AOSP搭建构建环境](https://source.android.com/source/initializing)

1. ##### 准备系统

   下载并安装`Ubuntu 16.4 LTS`

2. ##### 更换国内镜像源(可选)

   打开`software & updates`,选择最佳镜像源,随后`close`->`reload`->`install`->`reboot`.

3. ##### 更新软件

   ```shell
   $ sudo apt-get update
   ```

4. ##### 安装`openjdk`
   ```shell
   $ sudo apt-get install openjdk-8-jdk
   ```

   `JDK`支持的版本:
   
> Android **7.0** (Nougat) – Android **8.0** (Oreo)：
   > Ubuntu：OpenJDK **8**
   > Mac OS X：JDK 8u45 或更高版本

> Android **5.x **(Lollipop) – Android **6.0** (Marshmallow)：
   > Ubuntu：OpenJDK **7**
   > Mac OS X：jdk-7u71-macosx-x64.dmg

> Android **2.3.x** (Gingerbread) – Android **4.4.x** (KitKat)：
   > Ubuntu：Java JDK **6**
   > Mac OS X：Java JDK 6

> Android **1.5** (Cupcake) – Android **2.2.x** (Froyo)：
   > Ubuntu：Java JDK **5**



5. ##### 安装依赖包

   ```shell
   $ sudo apt-get install git-core gnupg flex bison build-essential zip curl zlib1g-dev gcc-multilib g++-multilib libc6-dev-i386 lib32ncurses5-dev x11proto-core-dev libx11-dev lib32z1-dev libgl1-mesa-dev libxml2-utils xsltproc unzip fontconfig
   ```

6. ##### 如需使用 SELinux 工具进行策略分析，您还需要安装 `python-networkx` 软件包

   ```shell
   $ sudo apt-get install python-networkx
   ```

7. ##### 如果您使用的是 LDAP 并且希望运行 ART 主机测试，还需要安装 `libnss-sss:i386` 软件包。

   ```shell
   $ sudo apt-get install libnss-sss:i386
   ```

8. ##### 开启缓存(可选)

   1. 安装`ccache`

       ```shell
       $ sudo apt-get install ccache
       ```

   2. 在`home`主目录的`.bashrc`中加入：

       ```
       export USE_CCACHE=1
       ```

   3. 指定缓存目录，也需要在`.bashrc`中加入，默认为当前用户目录下的.ccache

       ```
       export CCACHE_DIR=/home/fejq/.ccache
       ```

   4. 在修改了~/.bashrc 后记得source 下,或者直接重启虚拟机，不然刚刚添加的变量不会生效。

       ```shell
       $ source ~/.bashrc
       ```

   5. 配置缓存最大小，这个参数保存在`CCACHE_DIR`所在的目录	 

       ```shell
       # 配置缓存最大小
       $ ccache -M 100G
       ```

   6. 查看缓存状态

       ```shell
       $ ccache -s
       ```

### 下载源码

参考文献

>[source.android.com:下载源码](https://source.android.com/source/downloading)
>
>[清华大学开源软件镜像站:Android 镜像使用帮助](https://mirrors.tuna.tsinghua.edu.cn/)

1. ##### 安装`Repo`

   1. 确保主目录`/home`下有一个 bin/ 目录，并且该目录包含在路径中

       ```shell
       $ mkdir ~/bin
       # 添加bin目录到环境变量
       $ PATH=~/bin:$PATH
       # 打印环境变量
       $ echo $PATH
       ```
       
   2. 下载 `Repo` 工具，并确保它可执行
   
       ```shell
       curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
       chmod a+x ~/bin/repo
       ```
   
       > 可能需要更改DNS:
       >
       > ```shell
       > sudo gedit /etc/network/interfaces
       > ```
       >
       > 在末尾追加:
       >
       > ```
       > dns-nameservers 8.8.8.8
       > ```
   
   3. ##### 初始化仓库
   
       初始化仓库:
   
       ```shell
       repo init -u https://mirrors.tuna.tsinghua.edu.cn/git/AOSP/platform/manifest
       ```
   
       **如果提示无法连接到 gerrit.googlesource.com，请参照[git-repo的帮助页面](https://mirrors.tuna.tsinghua.edu.cn/help/git-repo)的更新一节。**
   
       如果需要某个特定的 Android 版本([列表](https://source.android.com/setup/start/build-numbers#source-code-tags-and-builds))：
   
       ```shell
       repo init -u https://mirrors.tuna.tsinghua.edu.cn/git/AOSP/platform/manifest -b android-8.0.0_r35
       ```
   
   4. ##### 同步源码树
   
       ```shell
       repo sync
       ```
   
       
       
       > 同步过程受网络状况影响,在同步过程中可能会出现错误,卡住不动等现象,此时,皆可由手动重新执行`repo sync`命令来解决,亦可通过以下脚本来自动执行
       >
       > 
       >
       > 新建脚本`repo-sync.sh`
       >
       >  
       >
       > **当出现错误而导致同步停止时,重新开始同步**
       >
       > ```shell
       > #!/bin/bash
       > echo "======= start repo sync ======="
       > # 进入同步目录(之前执行了repo init的目录) 
       > cd ~/AOSP/android-8.0.0_r35
       > repo sync -j4
       > while [ $? == 1 ]; do
       > echo "====== sync failed! re-sync again ====="
       > sleep 3
       > repo sync -j4
       > ```
       >
       > **当同步卡住不动时,重新开始同步**
       >
       > 安装网络监测工具`ifstat`
       >
       > ```shell
       > sudo apt-get install ifstat
       > ```
       >
       > 运行脚本
       >
       > ```shell
       > #!/bin/bash
       > 
       > #杀掉repo sync进程
       > kill_reposync() {
       >     PID=`ps aux |grep python|grep [r]epo |awk '{print $2}'`
       >     [[ -n $PID ]] && kill $PID
       > }
       > 
       > #启动reposync进程
       > start_reposync() {
       >     repo sync &
       > }
       > 
       > #重启reposync进程
       > restart_sync() {
       >     kill_reposync
       >     start_reposync
       > }
       > 
       > #网络检测相关阈值
       > th_net_value="30"    #实际检测，repo sync卡住时，网卡入口数据小于10
       > th_retry_times=100    #低于网络检测阈值次数限制
       > ((count_low=0))
       > 
       > restart_sync
       > 
       > 
       > while [[ 1 ]]; do
       >     # 用ifstat检测网速
       >     cur_speed=`ifstat 1 1 | tail -n 1 | awk '{print $1}'`
       > 
       >     result=$(echo "$cur_speed < $th_net_value" | bc)
       >     if [[ $result == "1" ]]; then
       >         ((count_low++))
       >     else
       >         ((count_low=0))
       >     fi
       >     if ((count_low > th_retry_times)); then
       >         ((count_low=0))
       >         echo "restart repo sync"
       >         restart_sync
       >     fi
       > done
       > ```
       >
       > 
       >
       > 参考:
       >
       > [关于使用repo时repo init和repo sync失败的一个解决方案](https://www.cnblogs.com/daimadebanyungong/p/7765218.html)
       
       > 需保证下载的源码,有对应的驱动支持
       >
       > [源代码标记](https://source.android.com/setup/start/build-numbers#source-code-tags-and-builds)
       >
       > [驱动支持](https://developers.google.cn/android/drivers)
       >
       > 如: 
       >
       > `Nexus 5X`有对 `Android 7.1.1`的驱动支持,代号为`NMF26F`:
       >
       > > Nexus 5X binaries for Android 7.1.1 (NMF26F)
       >
       > 搜索`build`号为`NMF26F`的源码版本,可以找到`android-7.1.1_r1`,即该版本的源码有对`Nexus 5X`的支持
   

### 开始编译

1. ##### 清理

   ```shell
   $ make clobber
   ```

2. ##### 设置环境

   ```shell
   $ source build/envsetup.sh
   ```

3. ##### 选择目标

   ```shell
   $ lunch
   ```

   ```shell
   $ lunch product_name-build_variant
   # eg:lunch aosp_bullhead-eng
   ```

4. ##### 开始编译

   ```shell
   $ time make -j4
   ```


