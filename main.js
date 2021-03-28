function test() {
    Java.perform(function () {
        Java.enumerateLoadedClasses({
            onMatch: function (className) {
                var klass = loadClass(className);
                var methods = klass.getMethods();
                fakeInvoke(methods);
            }, onComplete: function () {

            }
        })
    });
}

function aupk() {
    var date1 = new Date();
    var time1 = date1.getTime();

    Java.perform(function () {
        var class_ActivityThread = Java.use("android.app.ActivityThread");
        var array = new Array();
        var appClassLoader = class_ActivityThread.getClassLoader();
        Java.enumerateLoadedClasses({
            onMatch: function (className) {
                //console.log("class name:" + className);
                try {
                    array.push(className);
                }
                catch (error) {

                }
            }, onComplete: function () {

            }
        });
        for (var i = 0; i < array.length; i++) {
            try {
                console.log("进度:" + i + "/" + array.length);
                console.log("class name:" + array[i]);
                class_ActivityThread.aupkWithClassName(array[i]);
            }
            catch (error) {
                console.log("class error:" + array[i] + error);
                continue;
            }
        }
        var date2 = new Date();
        var time2 = date2.getTime();
        mapToFile();
        console.log("花费时间(秒):" + (time2 - time1) / 1000);
    })

}

function mapToFile() {
    Java.perform(function () {
        try {
            var class_DexFile = Java.use("dalvik.system.DexFile");
            class_DexFile.mapToFile();
        }
        catch (error) {
            console.log(error);
        }

    })
}

function aupkWithClassName() {
    Java.perform(function () {
        var class_ActivityThread = Java.use("android.app.ActivityThread");
        var result = class_ActivityThread.aupkThreadClasses("com.klcxkj.zqxy", "8273364_Execute_class.json");
        //var result2=class_ActivityThread.aupkThreadClasses("com.klcxkj.zqxy","9147744_Execute_class.json");

        console.log(result);
    })
}



function main() {
    Java.perform(function () {
        var class_ActivityThread = Java.use("android.app.ActivityThread");
        var mCount = class_ActivityThread.mCount.value;
        console.log("mCount:" + mCount);
    })

}

function sleep(time) {
    return new Promise((resolve) => setTimeout(resolve, time));
}

function test() {
    Java.perform(function () {
        var Toast = Java.use("android.widget.Toast");
        Toast.show.implementation = function () {
            console.log("show() called");
            this.show();
        }
    })
}

function printJavaStackTrace(name) {
    Java.perform(function () {
        var class_Exception = Java.use("java.lang,Exception");
        var obj_Exception = class_Exception.$new("Exception");
        var stackTraces = obj_Exception.getStackTrace();
        if (stackTraces != undefined && stackTraces != null) {
            var strStackTrace = stackTraces.toString();
            //var replaceStr=strStackTrace.replace(/,/g," \n ");
            logPrint("=======================" + name + " Stack trace start" + "=======================")
            logPrint(strStackTrace);
            logPrint("=======================" + name + " Stack trace end" + "=======================")
            class_Exception.$dispose();
        }
    })
}
//setImmediate(test);