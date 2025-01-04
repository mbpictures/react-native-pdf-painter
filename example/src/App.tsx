import { View, StyleSheet, Alert, Button } from 'react-native';
import { PdfAnnotationView } from 'react-native-pdf-annotation';
import { useState } from 'react';
import DocumentPicker, { types } from 'react-native-document-picker';

export default function App() {
    const [pdfFile, setPdfFile] = useState<string | null>(null);

    const handleSelectFile = async () => {
        try {
            const response = await DocumentPicker.pickSingle({
                type: types.pdf,
                copyTo: 'cachesDirectory',
            });
            setPdfFile(response.fileCopyUri);
        } catch (e) {
            Alert.alert('File Selection Error');
        }
    };

    return (
        <View style={styles.container}>
            <Button title={'Select PDF'} onPress={handleSelectFile} />
            {pdfFile && (
                <PdfAnnotationView pdfUrl={pdfFile} style={styles.box} />
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    box: {
        flex: 1,
    },
});
