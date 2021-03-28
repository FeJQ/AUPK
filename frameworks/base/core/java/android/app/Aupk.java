package android.app;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.lang.reflect.Constructor;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import android.app.Application;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import android.util.Log;
import android.util.LogPrinter;
import java.util.Map;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class Aupk
{
    class FileInfo
    {
        public String fileName;
        public List<ClassInfo> classList=new ArrayList<>();
    }
    class ClassInfo
    {
        public String className;
        public Map<String,Object> methodMap=new HashMap<String,Object>();
    }

    public static Object getFieldOjbect(String class_name, Object obj, String filedName)
    {
        try
        {
            Class obj_class = Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    
    public static ClassLoader getClassLoader()
    {
        ClassLoader resultClassloader = null;
        try
        {
            Class class_ActivityThread = Class.forName("android.app.ActivityThread");
            Method method = class_ActivityThread.getMethod("currentActivityThread", new Class[]{});
            Object currentActivityThread = method.invoke(null, new Object[]{});

            Object mBoundApplication = getFieldOjbect(
                    "android.app.ActivityThread",
                    currentActivityThread,
                    "mBoundApplication"
            );
            Object loadedApkInfo = getFieldOjbect(
                    "android.app.ActivityThread$AppBindData",
                    mBoundApplication,
                    "info"
            );
            Application mApplication = (Application) getFieldOjbect(
                    "android.app.LoadedApk",
                    loadedApkInfo,
                    "mApplication"
            );
            resultClassloader = mApplication.getClassLoader();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return resultClassloader;
    }


    public static Method getMethod(ClassLoader appClassLoader, String className, String methodName)
    {
        Class class_DexFileClazz = null;
        try
        {
            class_DexFileClazz = appClassLoader.loadClass(className);
            for (Method method : class_DexFileClazz.getDeclaredMethods())
            {
                if (method.getName().equals(methodName))
                {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    static Method method_native_fakeInvoke = null;
    /** 
     * 对集合里的每一个方法发起主动调用
     */
    public static void fakeInvoke(Object method)
    {
        try
        {
            // 该方法为native方法,声明于Aupk.java,实现于dalvik_system_DexFile.cc 
             // 用于对指定函数发起主动调用
            if(method_native_fakeInvoke==null)
            {
                method_native_fakeInvoke=getMethod(getClassLoader(),"android.app.Aupk","native_fakeInvoke");
            }   
            if(method==null)
            {
                return;
            }
            method_native_fakeInvoke.invoke(null, method);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
    }

    /**
     * 获取指定类里所有的方法
     * 返回类的方法数量
     */
    public static int loadAllMethodsWithClass(Class klass,ClassInfo classInfo)
    {
        //Log.i("AUPK", "ActivityThread:loadAllMethods from class:" + className);
        int count=0;
        try
        {
            if (klass == null)
            {
                return 0;
            }
            
            // 获取目标类的所有构造函数
            Constructor constructors[] = klass.getDeclaredConstructors();          
            for (Object constructor : constructors)
            {
                
                String methodName=klass.getName()+constructor.toString();
                classInfo.methodMap.put(methodName,constructor);
                count++;
            }

            // 获取目标类的所有成员函数
            Method[] methods = klass.getDeclaredMethods();
            for (Method method : methods)
            {
                String methodName=klass.getName()+method.toString();
                classInfo.methodMap.put(methodName,method);
                count++;
            }
        }
        catch (Error | Exception e)
        {
            e.printStackTrace();      
        } 
        return count;
    }     

  
       
    private static String formatTime(long ms) {
        int ss = 1000;
        int mi = ss * 60;
        int hh = mi * 60;
        int dd = hh * 24;

        long day = ms / dd;
        long hour = (ms - day * dd) / hh;
        long minute = (ms - day * dd - hour * hh) / mi;
        long second = (ms - day * dd - hour * hh - minute * mi) / ss;
        long milliSecond = ms - day * dd - hour * hh - minute * mi - second * ss;

        String strHour = hour < 10 ? "0" + hour : "" + hour;//小时
        String strMinute = minute < 10 ? "0" + minute : "" + minute;//分钟
        String strSecond = second < 10 ? "0" + second : "" + second;//秒
        String strMilliSecond = milliSecond < 10 ? "0" + milliSecond : "" + milliSecond;//毫秒
        strMilliSecond = milliSecond < 100 ? "0" + strMilliSecond : "" + strMilliSecond;
        return strHour+":"+strMinute + ":" + strSecond+",";//+strMilliSecond ;
    }

    static Thread thread=null;
    public synchronized void aupkThread()
    {      
        if(thread==null)
        {
            thread=new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    String configPath="data/local/tmp/aupk.config";
                    while(true)
                    {
                        try
                        {
                            Thread.sleep(1000);
                            String strConfig=readFileString(configPath);
                            if(strConfig!=null)
                            {
                                Log.e("AUPK", "Found configration:"+strConfig);
                                // 配置文件格式:
                                // com.package.name [method_info.json]
                                Log.e("AUPK", "Start aupk");
                                long startMillis = System.currentTimeMillis();

                                strConfig=strConfig.replace("\n","");
                                String[] configs = strConfig.split(" ");
                                if(configs.length==1)
                                {
                                    Log.i("AUPK","package name:"+configs[0]);
                                    aupkThreadClasses(configs[0],null);
                                }
                                else if(configs.length==2)
                                {
                                    Log.i("AUPK","package name:"+configs[0]);
                                    Log.i("AUPK","method info name:"+configs[1]);
                                    aupkThreadClasses(configs[0],configs[1]);
                                }          
                                else
                                {
                                    Log.e("AUPK", "Invalid configuration file:"+configPath);
                                    continue;
                                }                
                                
                                Log.e("AUPK", "Aupk run over");
                                long endMillis = System.currentTimeMillis();
                                String strTime=formatTime(endMillis-startMillis);
                                Log.e("AUPK","Time "+strTime);
                                File file = new File(configPath);
                                if(file.exists() && file.isFile())
                                {
                                    // 删除配置文件
                                    if(!file.delete())
                                    {
                                        Log.e("AUPK", "File:"+configPath+" delete failed");
                                        
                                    }
                                }
                                Log.e("AUPK", "Programe will kill the aupk thread");
                                thread=null;
                                break;
                            }
                        }  
                        catch(Exception e)
                        {
                            e.printStackTrace();

                        }                
                    }  
                    thread=null;
                }
            });
            thread.start();
        }
    }

    

    /**
     * 启动主动调用线程
     */
    public void unpackWithClassLoader(int second)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Log.e("AUPK", "start sleep for "+second+" seconds)......");
                    Thread.sleep(second* 1000);
                    Log.e("AUPK", "sleep over and start aupkThread");
                    aupkThread();
                    Log.e("AUPK", "aupk run over");
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }             
            }
        }).start();
    }
    
    private boolean aupkThreadClasses(String packageName,String dexClassFileName)
    {    
        List<FileInfo> fileList=new ArrayList<>();
        try
        {   
            //Map<MethodInfo,Object> methodMap=new HashMap<>();
            List<String> dexClassPaths=new ArrayList<>();   
            String folder="data/data/"+packageName+"/aupk/";        
            if(dexClassFileName==null)
            {
                File file=new File(folder);
                File[] files = file.listFiles();
                if(files==null)
                {
                    return false;
                }
                for(File f:files)
                {
                    if(f.getName().contains("_class.json"))
                    {
                        String fileName=f.getAbsolutePath();
                        //String fileName=folder+file.getName();
                        dexClassPaths.add(fileName);
                    }
                }
            }
            else
            {
                String fileName=folder+dexClassFileName;
                dexClassPaths.add(fileName);
            }

            Method method_mapToFile = getMethod(getClassLoader(),"android.app.Aupk","mapToFile");
            Log.i("AUPK","Found "+dexClassPaths.size()+" _class.json files");
            for (int i = 0; i <dexClassPaths.size() ; i++)
            {
                String dexClassPath=dexClassPaths.get(i);
                
                // 添加数据
                FileInfo fileInfo=new FileInfo();
                fileInfo.fileName=dexClassPath;
                fileList.add(fileInfo);

                //Log.i("AUPK","dex class path:"+dexClassPath);

                File classesFile = new File(dexClassPath);
                String strDexClass = readFileString(dexClassPath);
                if(strDexClass==null)
                {
                    continue;
                }
                JSONObject jsonDexClass = new JSONObject(strDexClass);
                if(jsonDexClass.has("count"))
                {
                    //int count = jsonDexClass.getInt("count");
                    //Log.i("AUPK","load classes file:"+dexClassPath+",count:"+count);
                }

                JSONArray data = jsonDexClass.getJSONArray("data");  
                //Log.i("AUPK","Load file["+(i+1)+"/"+dexClassPaths.size()+"]:"+classesFile.getName()+",count of class:"+data.length()); 

                for (int j = 0; j < data.length(); j++)
                {
                    try 
                    {
                        // 去掉开头L和结尾分号,并将/换成.
                        // Lcom/fighter/sdk/report/abtest/ABTestConfig$1;   =>   com.fighter.sdk.report.abtest.ABTestConfig$1
                        String className =data.getString(j).substring(1,data.getString(j).length()-1).replace("/",".");
                        if(className.equals("android.app.Aupk"))
                        {
                            continue;
                        }
                        ClassInfo classInfo=new ClassInfo();
                        classInfo.className=className;
                        fileInfo.classList.add(classInfo);

                        Class klass=getClassLoader().loadClass(className);
                        int count=loadAllMethodsWithClass(klass,classInfo);

                        Log.i("AUPK","Load file["+(i+1)+"/"+dexClassPaths.size()+"]:"+classesFile.getName()+",class["+(j+1)+"/"+data.length()+"]:"+className+",method count:"+count); 
                    } 
                    catch (Error | Exception e) 
                    {
                        e.printStackTrace();
                        continue;
                    }              
                }              
            }

            // 遍历方法map,并进行主动调用       
            for(int x=0;x<fileList.size();x++)
            {
                String fileName=fileList.get(x).fileName;
                List<ClassInfo> classList=fileList.get(x).classList;

                for(int y=0;y<classList.size();y++)
                {
                    String className=classList.get(y).className;
                    Map<String,Object> methodMap=classList.get(y).methodMap;

                    String log="File["+(x+1)+"/"+fileList.size()+"]:"+fileName+",";
                    log+="Class["+(y+1)+"/"+classList.size()+"]:"+className+",";
                    Log.i("AUPK",log);

                    for(Map.Entry<String,Object> entry :methodMap.entrySet())
                    {
                        try
                        {
                            String methodName=entry.getKey();
                            Object method=entry.getValue();
                            fakeInvoke(method);
                        }
                        catch(Error | Exception e)
                        {
                            e.printStackTrace();
                            continue;
                        }              
                    }
                }
                method_mapToFile.invoke(null); 
            }
            return true;             
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i("AUPK","aupkThreadClasses error");
        }
        return false;
    }

    /**
     * 读取配置文件
     */
    public static String readFileString(String fileName)
    {
        FileInputStream fis=null;
        try
        {
            File file = new File(fileName);
            fis = new FileInputStream(file);
            int length = fis.available();
            byte[] buffer = new byte[length];
            fis.read(buffer);
            String str = new String(buffer);
            fis.close();
            return str;
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            //Log.e("AUPK", "ActivityThread:readFileString,read config file failed:" +e.getMessage()+";fileName:"+fileName);
        }
       return null;
    }

    private static native void native_fakeInvoke(Object method);
    private static native void mapToFile();
}



