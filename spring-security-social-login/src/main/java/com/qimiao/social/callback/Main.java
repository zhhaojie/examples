package com.qimiao.social.callback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;


public class Main {

    public static void main(String[] args) {
        List<OItem> items = new ArrayList<>();
        items.add(new OItem("A", generateSortingIndex()));
        items.add(new OItem("B", generateSortingIndex()));
        items.add(new OItem("C", generateSortingIndex()));
        items.add(new OItem("D", generateSortingIndex()));
        items.add(new OItem("E", generateSortingIndex()));

        System.out.println("Before moving:");
        items.forEach(System.out::println);

        // 深度复制移动前的列表
        List<OItem> originalList = items.stream()
                .map(oItem -> new OItem(oItem.getTitle(), oItem.getSortingIndex()))
                .collect(Collectors.toList());

        String x = "B";
        int index = 0;
        System.out.println();

        System.out.println("将[" + x + "] >> 位置:" + index);
        moveTaskItem(items, x, index);

        x = "A";
        index = 0;
        System.out.println("将[" + x + "] >> 位置:" + index);
        moveTaskItem(items, x, index);

        System.out.println("\nAfter moving:");
        items.forEach(System.out::println);

        List<OItem> changedItems = getChangedItems(originalList, items);
        System.out.println("\nChanged Items:");
        System.out.println(changedItems);

    }

    private static LongAdder longAdder = new LongAdder();

    private static String generateSortingIndex() {
        long timestamp = System.nanoTime();
        int randomNum = ThreadLocalRandom.current().nextInt(1000);
        return String.valueOf(timestamp + randomNum);
    }

    public static void moveTaskItem(List<OItem> OItemItems, String title, int newPosition) {
        OItem itemToMove = OItemItems.stream()
                .filter(item -> item.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        // 更新该任务项的排序索引
        String newSortIndex = generateSortingIndex();
        itemToMove.setSortingIndex(newSortIndex);

        // 移动任务项到新的位置
        OItemItems.remove(itemToMove);
        OItemItems.add(newPosition, itemToMove);

        // 重新对任务项进行排序
        OItemItems.sort(Comparator.comparing(OItem::getSortingIndex));
    }

    // 获取发生变更的任务项
    public static List<OItem> getChangedItems(List<OItem> originalList, List<OItem> currentList) {
        return currentList.stream()
                .filter(item -> !originalList.contains(item))
                .collect(Collectors.toList());
    }

}