# FallingObjectsCompose
点击某处从点击处，弹出一个自定义图片或其它的物体，点击后弹出然后自由落体到底部，最后渐变消失


```kotlin name=FallingObjectsImageActivity.kt
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

// 定义一个数据类，保存每个掉落物体的信息
data class FallingObject(
    val id: Int, // 物体的唯一ID
    val initialX: Float, // 初始X坐标（点击位置的X坐标）
    val initialY: Float, // 初始Y坐标（点击位置的Y坐标）
    val rotation: Float, // 物体的旋转角度
    var stackHeight: Float = 0f, // 堆叠高度（用于计算物体是否堆叠在其他物体上）
)

class FallingObjectsImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FallingObjectsScreen() // 显示主界面
        }
    }
}

@Composable
fun FallingObjectsScreen() {
    val coroutineScope = rememberCoroutineScope() // 协程作用域，用于管理动画
    val screenHeight = remember { mutableStateOf(0f) } // 保存屏幕高度
    val objects = remember { mutableStateListOf<FallingObject>() } // 保存当前所有掉落物体的列表
    var objectId by remember { mutableStateOf(0) } // 自动递增的物体ID

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White) // 设置背景为白色
            .pointerInput(Unit) {
                detectTapGestures { offset -> // 检测屏幕点击事件
                    // 每次点击都会创建一个新的掉落物体
                    val newObject = FallingObject(
                        id = objectId++, // 为新物体分配唯一ID
                        initialX = offset.x, // 设置物体的初始X坐标为点击位置的X
                        initialY = offset.y, // 设置物体的初始Y坐标为点击位置的Y
                        rotation = Random.nextFloat() * 360f // 随机设置一个物体的旋转角度
                    )
                    objects.add(newObject) // 将新物体加入列表

                    // 启动掉落动画
                    coroutineScope.launch {
                        val animatableY = Animatable(offset.y)
                        animatableY.animateTo(
                            targetValue = screenHeight.value - 100f - newObject.stackHeight,
                            animationSpec = tween(durationMillis = 2000)
                        )
                        newObject.stackHeight += 50f // 更新堆叠高度
                    }
                }
            }
            .onGloballyPositioned { coordinates ->
                screenHeight.value = coordinates.size.height.toFloat() // 获取屏幕高度
            }
    ) {
        for (obj in objects) {
            FallingObjectComposable(
                obj = obj,
                screenHeight = screenHeight.value
            )
        }
    }
}

@Composable
fun FallingObjectComposable(
    obj: FallingObject, // 当前物体对象
    screenHeight: Float // 屏幕高度
) {
    val coroutineScope = rememberCoroutineScope() // 协程作用域
    val animatableY = remember { Animatable(obj.initialY) } // 动画控制的Y轴位置
    val animatableAlpha = remember { Animatable(1f) } // 动画控制的透明度，初始值为1（完全不透明）

    LaunchedEffect(obj.id) { // 使用物体的唯一ID来隔离动画状态
        // 执行物体的动画
        coroutineScope.launch {
            // 第一步：模拟“弹出”效果
            animatableY.animateTo(
                targetValue = obj.initialY - 150f, // 物体向上移动150像素
                animationSpec = tween(durationMillis = 300) // 动画时长300毫秒
            )
            // 第二步：模拟“自由落体”效果
            animatableY.animateTo(
                targetValue = screenHeight - 100f - obj.stackHeight, // 物体从最高点落到底部
                animationSpec = tween(durationMillis = 2000) // 动画时长2秒
            )

            // 等待1秒后，开始渐变消失动画
            delay(1000) // 延迟1秒
            animatableAlpha.animateTo(
                targetValue = 0f, // 透明度变为0（完全透明）
                animationSpec = tween(durationMillis = 1000) // 动画时长1秒
            )
        }
    }

    // 使用 Image 替代 Canvas 中的矩形
    Image(
        painter = painterResource(id = R.drawable.example_image), // 替换为你的图片资源
        contentDescription = "Falling Object",
        modifier = Modifier
            .size(50.dp) // 设置图片大小为50dp
            .graphicsLayer(
                alpha = animatableAlpha.value, // 根据透明度动画动态设置物体透明度
                rotationZ = obj.rotation, // 设置物体的随机旋转角度
                transformOrigin = TransformOrigin.Center // 设置旋转中心为图片中心
            )
            .offset {
                IntOffset(
                    (obj.initialX - 25.dp.toPx()).roundToInt(), // 确保图片中心与点击位置对齐
                    animatableY.value.roundToInt() // 动态设置Y轴位置
                )
            }
    )
}
```


### 效果预期

1. **点击屏幕**
   - 每次点击都会在点击位置生成一个图片。

2. **弹出动画**
   - 图片会从点击点向上弹出 150 像素。

3. **自由落体**
   - 图片从弹出的最高点自由落体到底部，始终保持垂直方向。

4. **旋转效果**
   - 图片会随机旋转，但旋转不会引起位置偏移。

5. **渐变消失**
   - 图片在底部停留 1 秒后，逐渐变透明并完全消失。


### 图片预览
![图片预览](https://qfx-img.pages.dev/v2/GnkR095.gif "图片预览")
