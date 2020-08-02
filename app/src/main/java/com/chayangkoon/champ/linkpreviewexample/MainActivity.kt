package com.chayangkoon.champ.linkpreviewexample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.chayangkoon.champ.linkpreview.LinkPreview
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = supervisorJob + Dispatchers.Main

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        progressBar.visibility = View.GONE
        Toast.makeText(this, throwable.message.orEmpty(), Toast.LENGTH_SHORT).show()
    }

    private val linkPreview = LinkPreview.Builder().build()
    private val links = listOf(
            "https://bit.ly/1sNZMwL",
            "https://en.wikipedia.org/wiki/Bitly",
            "http://developer.android.com/reference/java/net/HttpURLConnection",
            "https://i.pinimg.com/originals/60/c2/17/60c2177448cbf90408ed1df7da78cf00.jpg",
            "https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_480_1_5MG.mp4",
            "https://images.unsplash.com/photo-1593642702909-dec73df255d7?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=1050&q=80",
            "https://giphy.com/gifs/ggF7lWJROMXcfgXDox",
            "https://media.giphy.com/media/ggF7lWJROMXcfgXDox/giphy.gif",
            "https://i.giphy.com/media/JoV2BiMWVZ96taSewG/giphy.webp"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewLink(links.random())

        btnSubmit.setOnClickListener {
            val url = edtWebUrl.text.toString()
            if (url.isNotEmpty()) previewLinkWithCoroutine(url)
        }
        btnRandom.setOnClickListener {
            previewLinkWithRxJava3(links.random())
        }
    }

    private fun previewLink(url: String) {
        progressBar.visibility = View.VISIBLE
        linkPreview.loadPreview(url, {
            Glide.with(this).load(it.imageUrl).into(imvLinkPreviewImage)
            tvLinkPreviewUrl.text = it.canonicalUrl
            tvLinkPreviewTitle.text = it.title
            tvLinkPreviewDescription.text = it.description
            progressBar.visibility = View.GONE
        }, {
            progressBar.visibility = View.GONE
            Toast.makeText(this, it.message.orEmpty(), Toast.LENGTH_SHORT).show()
        })
    }

    private fun previewLinkWithCoroutine(url: String) {
        progressBar.visibility = View.VISIBLE
        launch(exceptionHandler) {
            val linkContent = withContext(Dispatchers.IO) {
                linkPreview.loadPreview(url)
            }
            Glide.with(this@MainActivity).load(linkContent.imageUrl).into(imvLinkPreviewImage)
            tvLinkPreviewUrl.text = linkContent.canonicalUrl
            tvLinkPreviewTitle.text = linkContent.title
            tvLinkPreviewDescription.text = linkContent.description
            progressBar.visibility = View.GONE
        }
    }

    private fun previewLinkWithRxJava3(url: String) {
        progressBar.visibility = View.VISIBLE
        val disposable = Single.fromCallable {
            linkPreview.loadPreview(url)
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Glide.with(this).load(it.imageUrl).into(imvLinkPreviewImage)
                    tvLinkPreviewUrl.text = it.canonicalUrl
                    tvLinkPreviewTitle.text = it.title
                    tvLinkPreviewDescription.text = it.description
                    progressBar.visibility = View.GONE
                }, {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, it.message.orEmpty(), Toast.LENGTH_SHORT).show()
                })
        compositeDisposable.add(disposable)
    }

    override fun onDestroy() {
        linkPreview.cancel()
        supervisorJob.cancel()
        compositeDisposable.dispose()
        super.onDestroy()
    }
}