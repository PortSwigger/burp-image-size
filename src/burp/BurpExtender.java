package burp;

import java.util.*;

public class BurpExtender implements IBurpExtender, IScannerCheck
{
	IExtensionHelpers helpers;
	IBurpExtenderCallbacks callbacks;

	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
	{
		callbacks.setExtensionName("Image size issues");
		callbacks.registerScannerCheck(this);
		this.callbacks = callbacks;
		this.helpers = callbacks.getHelpers();
	}

	@Override
	public List<IScanIssue> doPassiveScan(IHttpRequestResponse baseRequestResponse) {
		byte[] response = baseRequestResponse.getResponse();
		int offset = helpers.analyzeResponse(response).getBodyOffset();
		int bodyLength = response.length - offset;
		int[] d = SimpleImageSizeReader.getImageSize(response, offset, bodyLength);
		if (d == null) return null;
		IRequestInfo ri = helpers.analyzeRequest(baseRequestResponse.getHttpService(),
				baseRequestResponse.getRequest());
		String width = String.valueOf(d[0]);
		String height = String.valueOf(d[1]);
		IParameter widthParam = null, heightParam = null;
		for (IParameter param : ri.getParameters()) {
			String value = param.getValue();
			if (widthParam == null && width.equals(value)) {
				widthParam = param;
				if (heightParam != null) break;
			}
			else if (heightParam == null && height.equals(value)) {
				heightParam = param;
				if (widthParam != null) break;
			}
		}
		if (widthParam == null || heightParam == null) return null;
		// TODO if only width or height is affected, that'd be still an issue
		int[]  widthMarker = { widthParam.getValueStart(),  widthParam.getValueEnd()};
		int[] heightMarker = {heightParam.getValueStart(), heightParam.getValueEnd()};
		List<int[]> reqMarkers = widthMarker[0] < heightMarker[0] ?
			Arrays.asList(widthMarker, heightMarker) :
			Arrays.asList(heightMarker, widthMarker);
		return Collections.singletonList((IScanIssue)new ImageSizeIssue(
					callbacks.applyMarkers(baseRequestResponse, reqMarkers, null),
					ri.getUrl(), widthParam, heightParam));
	}

	@Override
	public List<IScanIssue> doActiveScan(IHttpRequestResponse baseRequestResponse,
			IScannerInsertionPoint insertionPoint) {
		return null;
	}

	@Override
	public int consolidateDuplicateIssues(IScanIssue existingIssue, IScanIssue newIssue) {
		return -1;
	}
}
