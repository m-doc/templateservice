var TemplateFile = React.createClass({
    render: function () {
        return (
            <div class="templateFile">
                <h4>{this.props.name}</h4>
                {this.props.sizeInBytes}
            </div>
        );
    }
});

var TemplateFileList = React.createClass({
    render: function () {

        var templates = this.props.data.map(function (template) {
            return <TemplateFile name={template.name} sizeInBytes={template.sizeInBytes}/>;
        });
        return (
            <div class="templateFileList">
                {templates}
            </div>
        );
    }
});

var data = [
    {"name": "template1", "sizeInBytes": 19345},
    {"name": "template2", "sizeInBytes": 9876}
];

ReactDOM.render(
    <TemplateFileList data={data}/>,
    document.getElementById("templateFileList")
);
