# Native PDF Viewer with drawing support

Display PDFs with hand-drawn annotation support on android and ios.

## Installation

```sh
npm install react-native-pdf-annotation
```

or

```sh
yarn add react-native-pdf-annotation
```

### WARNING
This library is fully written as fabric components and therefor **only** compatible with the new architecture!

For android uses: This libray uses androidx.ink in version 1.0.0alpha2, meaning that this library is not recommended for production use yet! Several issues have been discovered already (e.g. flickering when drawing, weird drawing behaviour when zoomed)

## Usage

Import the `PdfAnnotationView` component and assign a path to a pdf file to it. If you want to display the drawn annotations, add the `annotationFile` property.
```js
import { PdfAnnotationView } from "react-native-pdf-annotation";

<PdfAnnotationView
    pdfUrl={pathToPdfFile}
    annotationFile={pathToAnnotationFile}
    autoSave={true} // automatically save annotations on unmount to the annotationFile
/>
```

If you want to handle saving and loading of the annotations by yourself, use this example:

```ts
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

## How it works

### Android
This library uses the PdfRenderer class to render the pages of the pdf as a bitmap with a scaled to resolution (to make zoomed pages look crisp enough).
For drawing, the new androidx ink library is used.

### iOS
Uses PdfKit in combination with PencilKit to support drawing on pages.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
