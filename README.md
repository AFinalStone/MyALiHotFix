## 快速接入阿里的热修复

### 一、热修复需要修复的三个方面：

#### 1、类加载

类加载方案的原理是在App重新启动之后，让Classlader去加载新的类。因为在App运行到一半的时候，所有需要发生变更的类已经被加载过了，
在Android上是无法对一个类进行卸载的。如果不重启，原来的类还在虚拟机中，就无法加载心累。因此，只有在下次重启的时候，在还没有走到
业务逻辑之前抢先加载补丁中的新类，这样后续访问这个类时，就会Resolve为新类。从而达到热修复的目的。

#### 2、资源修复（包括Assets文件夹下的资源和Res文件夹下的资源）

目前市面上的很多资源热修复方案基本都是参考了Instant Run的实现。实际上，Instant Run的推出正是推动这次热修复浪潮的主因，
各家热修复方案，在代码、资源等方面的实现，很大程度上地参考了Instant Run 的代码，而资源热修复方案正式被拿来用到最多的地方。
简单说来，Instant Run 中的资源热修复分两步：

- 1>构造一个新的AssetManager，并通过反射调用AddAssetPath，把这个完整的新资源包加入到AssetManager中。这样就得到了一个包含所有
新资源的AssetManager。

- 2>找到所有之前引用到原有的AssetManager的地方，通过反射，把引用处替换为AssetManager。

#### 3、So库修复

 So库的修复本质上是对Native方法的修复和替换。
 Native方法的注册分两种：

 - 1>静态注册：

 - 2>动态注册：JNI-OnLoad


### 二、集成准备

#### 1、android studio集成方式

gradle远程仓库依赖, 打开项目找到app的build.gradle文件，添加如下配置：

添加maven仓库地址：

```gradle
repositories {
   maven {
       url "http://maven.aliyun.com/nexus/content/repositories/releases"
   }
}
```

添加gradle坐标版本依赖：
```gradle
compile 'com.aliyun.ams:alicloud-android-hotfix:3.0.6'
```

整体提出我的app下面的build.gradle：

```gradle
apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.0"
    defaultConfig {
        applicationId "com.shi.androidstudy.myalihotfix"
        minSdkVersion 16
        targetSdkVersion 22
        versionCode 2
        versionName "1.0.1"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    repositories {
        maven {
            url "http://maven.aliyun.com/nexus/content/repositories/releases"
        }
    }
}

dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    compile 'com.aliyun.ams:alicloud-android-hotfix:3.0.6'
    compile 'com.android.support:appcompat-v7:25.3.1'
    compile 'com.android.support.constraint:constraint-layout:1.0.2'
}
```

注意：传递性依赖utdid这个sdk, 所以不需要重复依赖utdid.但是另一方面其它阿里系SDK也可能依赖了utdid这个SDK,如果编译期间报utdid重复, 所以此时进行如下处理即可, 关闭传递性依赖:

```
compile ('com.aliyun.ams:alicloud-android-hotfix:3.0.6') {
     exclude(module:'alicloud-android-utdid')
}
```

utdid实际上是为设备生成唯一deviceid的一个基础类库

#### 2、权限说明

Sophix SDK使用到以下权限

```xml
<! -- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<! -- 外部存储读权限，调试工具加载本地补丁需要 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
READ_EXTERNAL_STORAGE/ACCESS_WIFI_STATE权限属于Dangerous Permissions,自行做好android6.0以上的运行时权限获取

```

#### 3、配置AndroidManifest文件

在AndroidManifest.xml中间的application节点下添加如下配置：

```xml
<meta-data
android:name="com.taobao.android.hotfix.IDSECRET"
android:value="App ID" />
<meta-data
android:name="com.taobao.android.hotfix.APPSECRET"
android:value="App Secret" />
<meta-data
android:name="com.taobao.android.hotfix.RSASECRET"
android:value="RSA密钥" />
```

将上述value中的值分别改为通过平台HotFix服务申请得到的App Secret和RSA密钥。

#### 4、混淆配置

```gradle
#基线包使用，生成mapping.txt
-printmapping mapping.txt
#生成的mapping.txt在app/buidl/outputs/mapping/release路径下，移动到/app路径下
#修复后的项目使用，保证混淆结果一致
#-applymapping mapping.txt
#hotfix
-keep class com.taobao.sophix.**{*;}
-keep class com.ta.utdid2.device.**{*;}
#防止inline
-dontoptimize
```

### 三、集成准备

initialize的调用应该尽可能的早，必须在Application.attachBaseContext()或者Application.onCreate()的最开始进行SDK初始化操作，否则极有可能导致崩溃。而查询服务器是否有可用补丁的操作可以在后面的任意地方。

```java
public  class MyApplication extends Application   {

    @Override
    public void onCreate() {
        super.onCreate();
        initALiHotFix();
    }

    // initialize最好放在attachBaseContext最前面,我这里放在了OnCreate
    public void initALiHotFix(){
       String appVersion =  SystemUtil.getCurrentAppVersionName(this);
        Toast.makeText(getApplicationContext(),"当前版本号："+appVersion ,Toast.LENGTH_SHORT).show();

        String AppId = "24531648-1";
        String AppSecret = "a29851323cdaea0ce836467e66e0efb3";
        String RsaSecret = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNQUrtuth5sykALLUKFrgN5N+xhSnvxhIR0KaantmT1wKsHOgFnzdK2YrSqxqsD1NPiXmiyvSvlWyV+YYiE6ZpxZFN7zHg8hEwEX4x0OusH6LeC8cxxHXPFSVXpaAMSX1to21Jkskg0Zkf33NY1hs1JXlAlXsj+KdkV5Xal88F2DkqaIvgrsKzAbHDVnPjvccz+7mRZ1AJ+BfPhJLUJalL06CgcgKfYHU3vYvbK0Jp5BeXtORVkP8FR+waU7WUzAkVFQd4MAIVqXTzYAjQAMP+Ry68t6pNjmXRF/8HXmGv0Ef8gJ9CZU0SMz8gs0UwcimapKpZy7dMMnknhcjfnYlbAgMBAAECggEAXiKo47kC2WXy0vKuIS9kQHMMqNUC88XquaLxFg7uiOBeiGNFgnaQHFMDWTVCKVFqCxto3uGoMPMd9vaWrwBGHVKQHqcqQBmlCl+redtwiuODhcTvGYMZ6Nyw4K7EZX46+VKvp8ObXOLkEHsh4sdneQtzvcwQyuGam+mTf8qKFwpN+CFjZo+33zHDD3K18TynSyIQXLpfZmahBR0PbrHaSuIN309/p8D+sNgRMyXYshHTvrdpf1J3npo5nOQm3IRW8MKQ9pZ5MGEDBCcVUPua1GkSSUcU/hEsrTe+/xX0eOHVfA+ztqeMoHdlWkt7xA47dqJTxXofZnve6jUFNOUsKQKBgQD2PwgHheUr+q+mdI/uvQGGg02ucY7YcRAUrz+ZWe9v26wts6G+ztvtDQmB2YOAdKRv7F4hsAyK0NaI33eSVD+59KxJFMNo+qVTvyphM0oSQS/7DfO3lNqqYg30nqjY6eKQv3DXs/PODSy9DSDu01bskTeOoyMPVJ1GVd5RgAUWFwKBgQDVYpsGeueVHGrK96Wfoa8SeCpfYudCqBeV3sfGucxmHOEDoNq7lszJoYlgVaAgq1/81g9BzSXjdI2GxWBX0HXuPj+6Ow7GaNXruS2yGs+Mhu5QSe+ucC+FyhV+kT4eewJVKhekjEK+PgfQEhUgTywfnqYCBNjfbPbAJLSYojR1XQKBgQDM1xAh2fMSy52UVUBqM5XyRIW5SEOwrxTWcBXyxkqUfWNUPSqepEt0fTTcbImksOMdK73+Pgmg7Cqaf3JjKmw8j1lGDdykFmSzLsHdS6IhX1K/gBKuM0hXFexQAi+pCZ5sFKSU+uAkFveRaDtuECYt8nsJz5FZrmSKXuHqYBlbDwKBgQDNPfbWmMi+x4KAjwqjLCT0otg+vyapGnz1Dj8hifxsC0Ly9njtDfMTzWgRXMqUIcJFsq8iH2xeBvFJu/ca/8suyHkLa1qexJ9eB9NICDmxdOcsGrGLGyTajrF198XEE5T+zWnIP5DC428oVvwzA3PxRetu7bKb1HbSAXwjg4DpkQKBgC6YtdIfxiTIfjpH15xG5f4opF0JY41N14YFlTGU748qd3LUF2IFpMu45gXNnwaFZNfMCxG6olXqTHicdfqbGS7Byxl0DAN0j6/TmoljBZqOr6MFlixl+g2pxWZk6edqOsp9v6X8p//75dVJOsaEAPrtV+0nbShU9fMiO0EzGosD";

        SophixManager.getInstance().setContext(this)
                //setAppVersion(appVersion)中的appVersion要和阿里云的控制台应用版本列表的版本号名称保持一致，够则会失败。
                .setAppVersion(appVersion)
                //setSecretMetaData(AppId, AppSecret, RsaSecret)中的三个Secret分别对应AndroidManifest里面的三个，可以不在AndroidManifest设置而是用此函数来设置Secret。
                // 放到代码里面进行设置可以自定义混淆代码，更加安全，此函数的设置会覆盖AndroidManifest里面的设置，如果对应的值设为null，默认会在使用AndroidManifest里面的。
                //注意，即使在这里使用代码配置了，AndroidManifest中仍然要配置
                .setSecretMetaData(AppId, AppSecret, RsaSecret)
                .setAesKey(null)
                .setEnableDebug(true)
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        // 补丁加载回调通知
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            Toast.makeText(getApplicationContext(),"CODE_LOAD_SUCCESS" ,Toast.LENGTH_SHORT).show();
                            // 表明补丁加载成功
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            // 表明新补丁生效需要重启. 开发者可提示用户或者强制重启;
                            // 建议: 用户可以监听进入后台事件, 然后应用自杀
                            Toast.makeText(getApplicationContext(),"CODE_LOAD_RELAUNCH" ,Toast.LENGTH_SHORT).show();
                        } else if (code == PatchStatus.CODE_LOAD_FAIL) {
                            // 内部引擎异常, 推荐此时清空本地补丁, 防止失败补丁重复加载
                            // SophixManager.getInstance().cleanPatches();
                            Toast.makeText(getApplicationContext(),"CODE_LOAD_FAIL" ,Toast.LENGTH_SHORT).show();
                        } else {
                            // 其它错误信息, 查看PatchStatus类说明
                            Toast.makeText(getApplicationContext(),"发生了其他错误，Code:"+code ,Toast.LENGTH_SHORT).show();
                        }
                    }
                }).initialize();
        // queryAndLoadNewPatch不可放在attachBaseContext 中，否则无网络权限，建议放在后面任意时刻，如onCreate中
        SophixManager.getInstance().queryAndLoadNewPatch();
    }
}
```

### 三、接口说明

#### 1、initialize方法

initialize(): <必选>

该方法主要做些必要的初始化工作以及如果本地有补丁的话会加载补丁, 但不会自动请求补丁。因此需要自行调用queryAndLoadNewPatch方法拉取补丁。这个方法调用需要尽可能的早, 推荐在Application的onCreate方法中调用, initialize()方法调用之前你需要先调用如下几个方法, 方法调用说明如下:

setContext(application): <必选> 传入入口Application即可

setAppVersion(appVersion): <必选> 应用的版本号

setSecretMetaData(idSecret, appSecret, rsaSecret): <可选> 三个Secret分别对应AndroidManifest里面的三个，可以不在AndroidManifest设置而是用此函数来设置Secret。放到代码里面进行设置可以自定义混淆代码，更加安全，此函数的设置会覆盖AndroidManifest里面的设置，如果对应的值设为null，默认会在使用AndroidManifest里面的。

setEnableDebug(true/false): <可选> 默认为false, 是否调试模式, 调试模式下会输出日志以及不进行补丁签名校验. 线下调试此参数可以设置为true, 查看日志过滤TAG:Sophix, 同时强制不对补丁进行签名校验, 所有就算补丁未签名或者签名失败也发现可以加载成功. 但是正式发布该参数必须为false, false会对补丁做签名校验, 否则就可能存在安全漏洞风险

setEnableFixWhenJit(): <可选> 默认情况下会在Android N以后的版本发现jit后跳过，这会使得部分7.0以上设备不进行修复。而如果想要此时不跳过，需要打开这个选项进行配置。打开后，需要做对Application进行改造。要尽可能避免Application类与和它同包名的类互相访问，如果确实需要访问，接口应设为public权限，详见常见问题文档，也可寻求群里技术支持解决。Android 7.0后带来的jit问题很隐蔽，只有频繁使用的app会由系统进行jit，该接口可以彻底解决Android N带来的jit问题。

setAesKey(aesKey): <可选> 用户自定义aes秘钥, 会对补丁包采用对称加密。这个参数值必须是16位数字或字母的组合，是和补丁工具设置里面AES Key保持完全一致, 补丁才能正确被解密进而加载。此时平台无感知这个秘钥, 所以不用担心阿里云移动平台会利用你们的补丁做一些非法的事情。

setPatchLoadStatusStub(new PatchLoadStatusListener()): <可选> 设置patch加载状态监听器, 该方法参数需要实现PatchLoadStatusListener接口, 接口说明见1.3.2.2说明

setUnsupportedModel(modelName, sdkVersionInt):<可选> 把不支持的设备加入黑名单，加入后不会进行热修复。modelName为该机型上Build.MODEL的值，这个值也可以通过adb shell getprop | grep ro.product.model取得。sdkVersionInt就是该机型的Android版本，也就是Build.VERSION.SDK_INT，若设为0，则对应该机型所有安卓版本。

#### 2、queryAndLoadNewPatch方法

该方法主要用于查询服务器是否有新的可用补丁. SDK内部限制连续两次queryAndLoadNewPatch()方法调用不能短于3s, 否则的话就会报code:19的错误码. 如果查询到可用的话, 首先下载补丁到本地, 然后

应用原本没有补丁, 那么如果当前应用的补丁是热补丁, 那么会立刻加载(不管是冷补丁还是热补丁). 如果当前应用的补丁是冷补丁, 那么需要重启生效.
应用已经存在一个补丁, 请求发现有新补丁后，本次不受影响。并且在下次启动时补丁文件删除, 下载并预加载新补丁。在下下次启动时应用新补丁。

补丁在后台发布之后, 并不会主动下行推送到客户端, 需要手动调用queryAndLoadNewPatch方法查询后台补丁是否可用.

只会下载补丁版本号比当前应用存在的补丁版本号高的补丁, 比如当前应用已经下载了补丁版本号为5的补丁, 那么只有后台发布的补丁版本号>5才会重新下载.
同时1.4.0以上版本服务后台上线了“一键清除”补丁的功能, 所以如果后台点击了“一键清除”那么这个方法将会返回code:18的状态码. 此时本地补丁将会被强制清除, 同时不清除本地补丁版本号

#### 3、killProcessSafely方法

可以在PatchLoadStatusListener监听到CODE_LOAD_RELAUNCH后在合适的时机，调用此方法杀死进程。注意，不可以直接Process.killProcess(Process.myPid())来杀进程，这样会扰乱Sophix的内部状态。因此如果需要杀死进程，建议使用这个方法，它在内部做一些适当处理后才杀死本进程。

#### 4、cleanPatches()方法

清空本地补丁，并且不再拉取被清空的版本的补丁。

#### 5、PatchLoadStatusListener接口

该接口需要自行实现并传入initialize方法中, 补丁加载状态会回调给该接口, 参数说明如下:

```
mode: 补丁模式, 0:正常请求模式 1:扫码模式 2:本地补丁模式
code: 补丁加载状态码, 详情查看PatchStatusCode类说明
info: 补丁加载详细说明, 详情查看PatchStatusCode类说明
handlePatchVersion: 当前处理的补丁版本号, 0:无 -1:本地补丁 其它:后台补丁
```

这里列举几个常见的code码说明,

```
code: 1 补丁加载成功
code: 6 服务端没有最新可用的补丁
code: 11 RSASECRET错误，官网中的密钥是否正确请检查
code: 12 当前应用已经存在一个旧补丁, 应用重启尝试加载新补丁
code: 13 补丁加载失败, 导致的原因很多种, 比如UnsatisfiedLinkError等异常, 此时应该严格检查logcat异常日志
code: 16 APPSECRET错误，官网中的密钥是否正确请检查
code: 18 一键清除补丁
code: 19 连续两次queryAndLoadNewPatch()方法调用不能短于3s
```

详情查看SDK中PatchStatus类的代码，有具体说明：

```java
package com.taobao.sophix;
public class PatchStatus {
    public final static int CODE_REQ_START = 0;
    public final static String INFO_REQ_START = "ready to start.";
    public final static int CODE_LOAD_SUCCESS = 1;
    public final static String INFO_LOAD_SUCCESS = "load new patch success.";
    public final static int CODE_ERR_NOTINIT = 2;
    public final static String INFO_ERR_NOTINIT = "didn't initialize hotfix sdk or initialize fail.";
    public final static int CODE_ERR_NOTMAIN = 3;
    public final static String INFO_ERR_NOTMAIN = "only allow query in main process.";
    public final static int CODE_ERR_INBLACKLIST = 4;
    public final static String INFO_ERR_INBLACKLIST = "current device does't support hotfix.";
    public final static int CODE_REQ_ERR = 5;
    public final static String INFO_REQ_ERR = "pull patch info detail fail, please check log.";
    public final static int CODE_REQ_NOUPDATE = 6;
    public final static String INFO_REQ_NOUPDATE = "there is not update.";
    public final static int CODE_REQ_NOTNEWEST = 7;
    public final static String INFO_REQ_NOTNEWEST = "the query patchversion equals or less than current patchversion, stop download.";
    public final static int CODE_DOWNLOAD_FAIL = 8;
    public final static int CODE_DOWNLOAD_SUCCESS = 9;
    public final static String INFO_DOWNLOAD_SUCCESS = "patch download success.";
    public final static int CODE_DOWNLOAD_BROKEN = 10;
    public final static String INFO_DOWNLOAD_BROKEN = "patch file is broken.";
    public final static int CODE_UNZIP_FAIL = 11;
    public final static String INFO_UNZIP_FAIL = "unzip patch file error, please check value of AndroidMenifest.xml RSASECRET or initialize param aesKey.";
    public final static int CODE_LOAD_RELAUNCH = 12;
    public final static String INFO_LOAD_RELAUNCH = "please relaunch app to load new patch.";
    public final static int CODE_LOAD_FAIL = 13;
    public final static String INFO_LOAD_FAIL = "load patch fail, please check stack trace of an exception: ";
    public final static int CODE_LOAD_NOPATCH = 14;
    public final static String INFO_LOAD_NOPATCH = "do not found any patch file to load.";
    public final static int CODE_REQ_APPIDERR = 15;
    public final static String INFO_REQ_APPIDERR = "appid is not found.";
    public final static int CODE_REQ_SIGNERR = 16;
    public final static String INFO_REQ_SIGNERR = "token is invaild, please check APPSECRET.";
    public final static int CODE_REQ_UNAVAIABLE = 17;
    public final static String INFO_REQ_UNAVAIABLE = "req is unavailable as has already been in arrearage.";
    public final static int CODE_REQ_CLEARPATCH = 18;
    public final static String INFO_REQ_CLEARPATCH = "clean client patch as server publish clear cmd.";
    public final static int CODE_REQ_TOOFAST = 19;
    public final static String INFO_REQ_TOOFAST = "two consecutive request should not short then 3s.";
    public final static int CODE_PATCH_INVAILD = 20;
    public static final String INFO_PATCH_INVAILD = "patch invaild, as patch not exist or is dir or not a jar compress file.";
    public final static int CODE_LOAD_FORBIDDEN = 21;
    public static final String INFO_LOAD_FORBIDDEN = "debuggable is false! forbid loading local patch for secure reason!";
}
```

### 五、AndroidManifest

其中的meta-data的IDSECRET、APPSECRET、RSASECRET没有在这里填充，出于安全考虑，把他们放在代码初始化中填充，后期再进行混淆和加密，更安全。

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.shi.androidstudy.myalihotfix">

    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <!-- 外部存储读权限，调试工具加载本地补丁需要 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:name=".MyApplication"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <meta-data
            android:name="com.taobao.android.hotfix.IDSECRET"
            android:value="" />
        <meta-data
            android:name="com.taobao.android.hotfix.APPSECRET"
            android:value="" />
        <meta-data
            android:name="com.taobao.android.hotfix.RSASECRET"
            android:value="" />

    </application>

</manifest>
```



### 六、版本管理说明

#### 说明一：patch是针对客户端具体某个版本的，patch和具体版本绑定

- eg. 应用当前版本号是1.1.0, 那么只能在后台查询到1.1.0版本对应发布的补丁, 而查询不到之前1.0.0旧版本发布的补丁.

#### 说明二：针对某个具体版本发布的新补丁, 必须包含所有的bugfix, 而不能依赖补丁递增修复的方式, 因为应用仅可能加载一个补丁

- eg. 针对1.0.0版本在后台发布了一个补丁版本号为1的补丁修复了bug1, 然后发现此时针对这个版本补丁1修复的不完全, 代码还有bug2, 在后台重新发布一个补丁版本号为2的补丁, 那么此时补丁2就必须同时包含bug1和bug2的修复才行, 而不是只包含bug2的修复(bug1就没被修复了)












