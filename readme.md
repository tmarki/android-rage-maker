# About Android Rage Maker #

Android Rage Maker is an application for Android that lets you create rage comics while on the move.
It supports external image packs, object manipulation and drawing, among other things.

# Usage #

## Interaction modes ##

There are four modes of interaction.

| ![http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/hand.png](http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/hand.png) | **Manipulate** | tap to select an image or text object, then drag it, and resize it by using the resize box or by pinching. You can also rotate it with a two fingered rotating motion. If there are no image objects selected, dragging will pan the canvas, while pinching will zoom it. |
|:----------------------------------------------------------------------------------------------------------------------------------------------------|:---------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/line.png](http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/line.png) |  **Line** | draw straight lines. Tap for the line start, then move your finger to select the line's end point. |
| ![http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/pencil.png](http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/pencil.png) | **Free draw** | draw free style lines with your finger |
| ![http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/text.png](http://android-rage-maker.googlecode.com/git/res/drawable-mdpi/text.png) | **Text** | add a text object. Tap once to select the text position. Enter the desired text in the pop-up box. When you click OK, a new text object will be inserted into the comic at the position you tapped. |

The current interaction mode is always indicated by the icon on the bottom right corner of the screen. Tap this icon to change the mode.

## Layers ##

The canvas has the following layers. They are drawn on top of each other, in the following order:

  1. _Background image layer_ - all image and text objects are added to this layer by default.
  1. _Lines layer_ - the line and free drawings are shown on this layer.
  1. _Foreground image layer_ - to bring image or text objects to this layer you must select the object in **manipulate** mode, then long-press to bring up the context menu, and select "Bring to front". Also use the context menu to send an image back to the background layer. You can use the _SPACE_ button on your keypad to quickly do this.

## Text and line options ##

In **line**, **free draw** and **text** modes the application menu will contain Pen/Text color options that lets you select the color of these operations. Note that this also changes the last drawn line or last inserted text's color.

In **line** and **free draw** mode the menu contains a Pen width option that lets you select the width of the brush.

In **text** mode the Text type option lets you select the font.

## Keyboard shortcuts ##

The application has a number of keyboard shortcuts:

| **Directional pad** | move object/canvas |
|:--------------------|:-------------------|
| **W**/**S** | scale object/canvas |
| **A**/**D** | rotate object |
| **SPACE** | move object to front or send to back |
| **BACK** | undo |
| **R** | redo (only available right after undo moves) |
| **BACKSPACE** | delete selected object |