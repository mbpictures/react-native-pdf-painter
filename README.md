<h1 align="center">
    Welcome to react-native-pdf-annotation👋<br />
</h1>
<h3 align="center">
    Your React Native PDF Viewer with native annotation support
</h3>
<p align="center">
    <a href="LICENSE" target="_blank">
      <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge" />
    </a>
    <img alt="Build Status" src="https://img.shields.io/github/actions/workflow/status/mbpictures/react-native-pdf-annotation/ci.yml?style=for-the-badge" />
</p>

> Easy-to-use react native component for efficient displaying of PDFs, with finger and pen support for hand drawing. Supports PencilKits ToolPicker out-of-the-box on iOS.

<div style="display: flex; flex-direction: row; justify-content: center">
    <div style="margin-right: 5px;">
        <img src="docs/demo-android.gif" alt="Demo Android" />
        <p align="center">Android</p>
    </div>
    <div style="margin-left: 5px;">
        <img src="docs/demo-ios.gif" alt="Demo iOS" />
        <p align="center">iOS</p>
    </div>
</div>

## 📥 Installation

```sh
npm install react-native-pdf-annotation
```

or

```sh
yarn add react-native-pdf-annotation
```

### iOS
```sh
cd ios && pod install
```

### WARNING
This library is completely written as Fabric components and is therefore **only** compatible with the new architecture!

For Android users: This library uses androidx.ink in version 1.0.0alpha2, which means that this library is not recommended for production use yet! Some issues have already been discovered (e.g. flickering when drawing)

For iOS users: Annotations only work for iOS versions >= 16, everything below can view PDFs but not draw.

## ▶️ Usage

Import the `PdfAnnotationView` component and assign a path to a pdf file to it. If you want to display the drawn annotations, add the `annotationFile` property.
```jsx
import { PdfAnnotationView } from "react-native-pdf-annotation";

<PdfAnnotationView
    pdfUrl={pathToPdfFile}
    annotationFile={pathToAnnotationFile}
    autoSave={true} // automatically save annotations on unmount to the annotationFile
/>
```

You can handle saving and loading of the annotations manually by using the `saveAnnotations` and `loadAnnotations` methods:

```tsx
import { PdfAnnotationView, type Handle } from "react-native-pdf-annotation";

const Component = () => {
    const ref = useRef<Handle>(null);

    const saveAnnotations = () => ref.current?.saveAnnotations(FILE_PATH);
    const loadAnnotations = () => ref.current?.loadAnnotations(FILE_PATH);

    return (
        <PdfAnnotationView
            ref={ref}
            pdfUrl={pathToPdfFile}
            annotationFile={pathToAnnotationFile}
            autoSave={true} // automatically save annotations on unmount to the annotationFile
        />
    );
}

```

Refer to [example](example/src/App.tsx) for detailed usage example.

## Props & Methods

| Name                 | Platform     | Description                                                                                                            |
|----------------------|--------------|------------------------------------------------------------------------------------------------------------------------|
| pdfUrl               | ios, android | Local URL of the PDF file                                                                                              |
| annotationsFile      | ios, android | Local URL of the files used for annotations (file extension doesn't matter)                                            |
| thumbnailMode        | ios, android | Displays only the first page without interaction                                                                       |
| autoSave             | android      | Automatically save file after changing pdf url or unmounting the component                                             |
| brushSettings        | ios, android | Pass undefined to disable drawing and pass an `BrushSettings` object to enable drawing with the provided configuration |
| hidePagination       | android      | Disable the pagination buttons at the bottom                                                                           |
| iosToolPickerVisible | ios          | Show/Hide the PencilKit ToolPicker                                                                                     |

**Info**: The ToolPicker is fixed and always at the bottom of the iPhone screen! Keep this in mind when designing your PDF Viewer screen!

### `saveAnnotations(filePath: string): void`
Save the current drawings on all pages to the provided file path.

### `loadAnnotations(filePath: string): void`
Load the drawings of all pages from the provided file path.

### `undo(): void`
Undo the last change on the current page

### `redo(): void`
Redo the last undone change on the current page

### `clear(): void`
Delete all drawings on the current page

## How it works

### Android
This library uses the PdfRenderer class to render the pages of the pdf as a bitmap, scaled to a higher resolution (to make zoomed pages look sharp enough). For drawing, the new androidx ink library is used.

### iOS
Uses PdfKit in conjunction with PencilKit to support drawing on pages.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
