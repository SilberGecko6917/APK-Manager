package me.geckotv.apkmanager.feature.apps

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons

/** Size of the icon in list items. */
internal val ListIconSize = 40.dp

/** Size of the icon in sheet headers. */
internal val HeaderIconSize = 56.dp

private const val ICON_CACHE_SIZE = 64

/** Icons of installed apps by package name and size in pixels, so scrolling a list does not load them again. */
private val iconCache = LruCache<String, ImageBitmap>(ICON_CACHE_SIZE)

/**
 * The launcher icon of the installed app [packageName] as a circle. The icon is loaded off the main
 * thread; [fallback] is shown until it is there, and if the app is not installed.
 */
@Composable
internal fun InstalledAppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = ListIconSize,
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val key = "$packageName@$sizePx"
    val icon by produceState(initialValue = iconCache[key], key) {
        if (value == null) {
            value =
                withContext(Dispatchers.IO) { loadIcon(context, packageName, sizePx) }
                    ?.also { iconCache.put(key, it) }
        }
    }
    val loaded = icon
    if (loaded == null) {
        Box(modifier = modifier) { fallback() }
    } else {
        Image(
            bitmap = loaded,
            contentDescription = null,
            modifier = modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Fit,
        )
    }
}

private fun loadIcon(
    context: Context,
    packageName: String,
    sizePx: Int,
): ImageBitmap? =
    try {
        context.packageManager
            .getApplicationIcon(packageName)
            .toBitmap(width = sizePx, height = sizePx)
            .asImageBitmap()
    } catch (ignored: PackageManager.NameNotFoundException) {
        null
    }

/**
 * The avatar of a repository owner as a circle. Without an [avatarUrl], or until the image is
 * loaded, an app icon is shown.
 */
@Composable
internal fun OwnerAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = ListIconSize,
) {
    Box(modifier = modifier.size(size)) {
        SectionItemIcon(ApkManagerIcons.Apps, modifier = Modifier.size(size))
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * The icon of a tracked app: the installed app's icon if the app is installed, otherwise the avatar
 * of the repository owner.
 */
@Composable
internal fun AppIcon(
    packageName: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = ListIconSize,
) {
    if (packageName == null) {
        OwnerAvatar(avatarUrl = avatarUrl, modifier = modifier, size = size)
    } else {
        InstalledAppIcon(packageName = packageName, modifier = modifier, size = size) {
            OwnerAvatar(avatarUrl = avatarUrl, size = size)
        }
    }
}
