# LinkPreview
A link preview library for android

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
