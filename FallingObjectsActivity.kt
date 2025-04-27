import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


// 定义一个数据类，保存每个掉落物体的信息
data class FallingObject(
    val id: Int, // 物体的唯一ID
    val initialX: Float, // 初始X坐标（点击位置的X坐标）
    val initialY: Float, // 初始Y坐标（点击位置的Y坐标）
    val rotation: Float, // 物体的旋转角度
    var stackHeight: Float = 0f, // 堆叠高度（用于计算物体是否堆叠在其他物体上）
)

class FallingObjectsActivity : ComponentActivity() {
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
            .background(Color.White) // 设置背景为白色
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

                    // 开启一个协程执行物体的动画
                    coroutineScope.launch {
                        val animatableY = Animatable(offset.y) // 动画控制的Y轴位置，初始值为点击的Y坐标
                        // 第一步：模拟“弹出”效果，即物体向上移动150像素
                        animatableY.animateTo(
                            targetValue = offset.y - 150f, // 向上移动到点击位置-150像素
                            animationSpec = tween(durationMillis = 300) // 动画时长300毫秒
                        )
                        // 第二步：模拟“自由落体”效果
                        animatableY.animateTo(
                            targetValue = screenHeight.value - 100f - newObject.stackHeight, // 物体落到底部并考虑堆叠高度
                            animationSpec = tween(durationMillis = 2000) // 动画时长2秒
                        )
                        newObject.stackHeight += 50f // 更新堆叠高度，给下一个物体留位置
                    }
                }
            }
            .onGloballyPositioned { coordinates ->
                screenHeight.value = coordinates.size.height.toFloat() // 获取屏幕高度
            }
    ) {
        // 循环渲染每个掉落的物体
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
    val rotation = obj.rotation // 物体的旋转角度

    LaunchedEffect(Unit) {
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

    Canvas(
        modifier = Modifier
            .size(50.dp) // 设置物体大小为50dp
            .graphicsLayer(alpha = animatableAlpha.value) // 根据透明度动画动态设置物体透明度
            .offset { IntOffset(obj.initialX.toInt(), animatableY.value.toInt()) } // 根据Y轴动画位置设置物体位置
    ) {
        rotate(rotation) { // 设置物体的随机旋转角度
            drawRect(
                color = Color.Red, // 画一个红色的矩形
                topLeft = Offset.Zero, // 矩形的起始位置
                size = Size(50f, 50f) // 矩形的大小
            )
        }
    }
}
