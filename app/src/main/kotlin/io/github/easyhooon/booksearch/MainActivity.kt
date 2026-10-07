package io.github.easyhooon.booksearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.easyhooon.booksearch.core.ui.theme.BookSearchTheme
import soil.query.compose.SwrClientProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appGraph = (application as BookSearchApplication).appGraph
        setContent {
            BookSearchTheme {
                SwrClientProvider(appGraph.swrClient) {
                    BookSearchApp(appGraph = appGraph)
                }
            }
        }
    }
}
