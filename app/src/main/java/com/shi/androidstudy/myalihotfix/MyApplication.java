package com.shi.androidstudy.myalihotfix;

import android.app.Application;
import android.widget.Toast;

import com.taobao.sophix.PatchStatus;
import com.taobao.sophix.SophixManager;
import com.taobao.sophix.listener.PatchLoadStatusListener;

/**
 * Created by AFinalStone on 2017/7/19.
 * 邮箱：602392033@qq.com
 */

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
