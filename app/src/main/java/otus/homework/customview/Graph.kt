package otus.homework.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import kotlin.math.roundToInt

private const val BASE_SIZE_DP = 200f
private const val GRAPH_INDENT_DP = 4f

class Graph @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {
    private var DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

    private val BASE_SIZE_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BASE_SIZE_DP, context.resources.displayMetrics).roundToInt()
    private val graphIndentPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GRAPH_INDENT_DP, context.resources.displayMetrics)

    init {

    }

    private var maxAmountText = ""
    private var startDateText = ""
    private var endDateText = ""


    private var dataPoints = mutableListOf<DataPoint>()

    private data class DataPoint(
        var proportion: Float = 0f,
        var timeProportion: Float = 0f,
        var proportionPx: Float = 0f,
        var timeProportionPx: Float = 0f,
        var dayNum: Long = 0,
        var amount: Int = 0,
    )

    fun setCharges(charges: List<Charge>, startDate: LocalDate, endDate: LocalDate, maxAmount: Int) {
        val daysCount = endDate.toEpochDay() - startDate.toEpochDay()

        val dateToPointMap = mutableMapOf<LocalDate, DataPoint>()
        for (charge in charges) {
            val date = charge.time.fromEpochSecondToLocalDate()
            if (startDate.isAfter(date) || endDate.isBefore(date)) continue

            val point: DataPoint = dateToPointMap.get(date) ?: run {
                val dayNum = date.toEpochDay() - startDate.toEpochDay()

                DataPoint(
                    dayNum = dayNum,
                    timeProportion = dayNum.toFloat() / daysCount
                ).apply {
                    dateToPointMap.put(date, this)
                }
            }

            point.amount += charge.amount
        }

        if (dateToPointMap.isEmpty()) {
            dataPoints.clear()
            invalidate()
            return
        }

//        val maxAmount = dateToPointMap.values.maxOf { it.amount }// ?: maxAmount

        dataPoints = dateToPointMap.values
            .sortedBy { it.timeProportion }
            .onEach {
                it.proportion = 1f - it.amount.toFloat() / maxAmount
                it.proportionPx = if (height == 0) 0f else (height - 2 * graphIndentPx) * it.proportion + graphIndentPx
                it.timeProportionPx = if (width == 0) 0f else (width - 2 * graphIndentPx) * it.timeProportion + graphIndentPx
            }
            .toMutableList()

        maxAmountText = context.getString(R.string.graph_max_amount, maxAmount)
        startDateText = DATE_FORMATTER.format(startDate)
        endDateText = DATE_FORMATTER.format(endDate)

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = View.getDefaultSize(BASE_SIZE_PX, widthMeasureSpec)
        val height = View.getDefaultSize(BASE_SIZE_PX, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)

        for (dataPoint in dataPoints) {
            dataPoint.proportionPx = (height - 2 * graphIndentPx) * dataPoint.proportion + graphIndentPx
            dataPoint.timeProportionPx = (width - 2 * graphIndentPx) * dataPoint.timeProportion + graphIndentPx
        }
    }

    private val pointsPaint = Paint().apply {
        this.color = Color.BLACK
        this.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, context.resources.displayMetrics)
        this.style = Paint.Style.FILL_AND_STROKE
        this.isAntiAlias = true
    }

    private val onePointPaint = Paint().apply {
        this.color = Color.BLACK
        this.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, context.resources.displayMetrics)
        this.style = Paint.Style.FILL_AND_STROKE
        this.isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        this.color = Color.GRAY
        this.style = Paint.Style.STROKE
        this.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        this.isAntiAlias = true
    }

    private val lineTextPaint = Paint().apply {
        this.color = Color.GRAY
        this.style = Paint.Style.FILL
        this.isAntiAlias = true
        this.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
        this.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawLine(graphIndentPx, graphIndentPx, width - graphIndentPx, graphIndentPx, linePaint)
        canvas.drawLine(graphIndentPx, height - graphIndentPx, width - graphIndentPx, height - graphIndentPx, linePaint)
        canvas.drawLine(graphIndentPx, graphIndentPx, graphIndentPx, height - graphIndentPx, linePaint)
        canvas.drawLine(width - graphIndentPx, graphIndentPx, width - graphIndentPx, height - graphIndentPx, linePaint)

        val textWidth = lineTextPaint.measureText(maxAmountText)

        canvas.drawText(maxAmountText, width - graphIndentPx - textWidth, graphIndentPx + lineTextPaint.textSize, lineTextPaint)
        canvas.drawText(startDateText, graphIndentPx, height - graphIndentPx * 2, lineTextPaint)
        canvas.drawText(endDateText, width - graphIndentPx - lineTextPaint.measureText(endDateText), height - graphIndentPx * 2, lineTextPaint)

        var pointX = 0f
        when (dataPoints.size) {
            0 -> Unit
            1 -> {
                val point = dataPoints.first()
                canvas.drawPoint(point.timeProportionPx, point.proportionPx, onePointPaint)
            }
            else -> {
                for (i in 0 until dataPoints.lastIndex) {
                    pointX += dataPoints[i].timeProportionPx

                    canvas.drawLine(
                        pointX,
                        dataPoints[i].proportionPx,
                        pointX + dataPoints[i + 1].timeProportionPx,
                        dataPoints[i + 1].proportionPx,
                        pointsPaint
                    )
                }
            }
        }
    }
}