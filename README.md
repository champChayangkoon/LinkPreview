
<img src="https://i.imgur.com/me95sRI.gif" alt="link preview" width="400" />

## Gradle
Add dependencies : 
```groovy
implementation 'com.chayangkoon.champ:linkpreview:1.0.0'
```
## Usage
```kotlin
private val linkPreview = LinkPreview.Builder().build()

linkPreview.loadPreview(url, {
 // handle link content
}, {
 // handle exception
})
```

**Cancel unfinished load preview**
If you are using LinkPreview with Activity, Fragment or ViewModel, it is important to cancel unfinished load preview at the end of the lifecycle.

```kotlin
linkPreview.cancel()

```
But you use LinkPreview with Kotlin Coroutine or RxJava, It is not necessary to use **linkPreview.cancel()** 

**Kotlin Coroutine**
```kotlin
private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
  // handle exception
}

coroutineScope.launch(exceptionHandler) {
    val linkContent = withContext(Dispatchers.IO) {
        linkPreview.loadPreview(url)
    }
    // handle link content
}
```

**RxJava**
```java
private val compositeDisposable: CompositeDisposable = CompositeDisposable()

val disposable = Single.fromCallable {
    linkPreview.loadPreview(url)
}.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          // handle link content
        }, {
          // handle exception
        })
compositeDisposable.add(disposable)
```
## License
```
Copyright 2020 Chayangkoon Tirawanon

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
