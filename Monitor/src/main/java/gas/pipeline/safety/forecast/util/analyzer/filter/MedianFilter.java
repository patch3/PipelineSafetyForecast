package gas.pipeline.safety.forecast.util.analyzer.filter;

import gas.pipeline.safety.forecast.util.model.AnomalyModel;
import lombok.val;

import java.util.Map;
import java.util.PriorityQueue;

public class MedianFilter extends FilterStrategy {
    @Override
    public double applyFilter(AnomalyModel model, double value) {
        model.measurements.add(value);

        if (model.maxHeap.isEmpty() || value <= model.maxHeap.peek()) {
            model.maxHeap.offer(value);
        } else {
            model.minHeap.offer(value);
        }

        // Удаление старого элемента при переполнении окна
        if (model.measurements.size() > model.sizeWindow) {
            val oldest = model.measurements.poll();
            model.expired.put(oldest, model.expired.getOrDefault(oldest, 0) + 1);
        }

        // Удаление устаревших элементов из куч
        pruneHeap(model.maxHeap, model.expired);
        pruneHeap(model.minHeap, model.expired);

        // Балансировка куч
        while (model.maxHeap.size() > model.minHeap.size() + 1) {
            model.minHeap.offer(model.maxHeap.poll());
        }
        while (model.minHeap.size() > model.maxHeap.size()) {
            model.maxHeap.offer(model.minHeap.poll());
        }

        // Вычисление медианы
        if (model.maxHeap.isEmpty() && model.minHeap.isEmpty()) {
            return 0.0;
        }

        if (model.maxHeap.size() == model.minHeap.size()) {
            return (model.maxHeap.peek() + model.minHeap.peek()) / 2.0;
        } else {
            return model.maxHeap.peek();
        }
    }

    private void pruneHeap(PriorityQueue<Double> heap, Map<Double, Integer> expired) {
        while (!heap.isEmpty() && expired.getOrDefault(heap.peek(), 0) > 0) {
            val val = heap.poll();
            int count = expired.get(val);
            if (count == 1) {
                expired.remove(val);
            } else {
                expired.put(val, count - 1);
            }
        }
    }
}