// jQuery File Tree Plugin
//
// Version 1.01
//
// Cory S.N. LaViska
// A Beautiful Site (http://abeautifulsite.net/)
// 24 March 2008
//
// Visit http://abeautifulsite.net/notebook.php?article=58 for more information
//
// Usage: $('.fileTreeDemo').fileTree( options, callback )
//
// Options:  root           - root folder to display; default = /
//           script         - location of the serverside AJAX file to use; default = jqueryFileTree.php
//           folderEvent    - event to trigger expand/collapse; default = click
//           expandSpeed    - default = 500 (ms); use -1 for no animation
//           collapseSpeed  - default = 500 (ms); use -1 for no animation
//           expandEasing   - easing function to use on expand (optional)
//           collapseEasing - easing function to use on collapse (optional)
//           multiFolder    - whether or not to limit the browser to one subfolder at a time
//           loadMessage    - Message to display while initial tree loads (can be HTML)
//
// History:
//
// 1.01 - updated to work with foreign characters in directory/file names (12 April 2008)
// 1.00 - released (24 March 2008)
//
// TERMS OF USE
// 
// This plugin is dual-licensed under the GNU General Public License and the MIT License and
// is copyright 2008 A Beautiful Site, LLC. 
//
if(jQuery) (function($){
	
	$.extend($.fn, {
		fileTree: function(o, h) {
			console.log("filetree");
			// Defaults
			if( !o ) var o = {};
			if( o.root == undefined ) o.root = '/';
			if( o.script == undefined ) o.script = 'jqueryFileTree.php';
			if( o.folderEvent == undefined ) o.folderEvent = 'click';
			if( o.expandSpeed == undefined ) o.expandSpeed= 500;
			if( o.collapseSpeed == undefined ) o.collapseSpeed= 500;
			if( o.expandEasing == undefined ) o.expandEasing = null;
			if( o.collapseEasing == undefined ) o.collapseEasing = null;
			if( o.multiFolder == undefined ) o.multiFolder = true;
			if( o.loadMessage == undefined ) o.loadMessage = 'Loading...';
			if( o.init == undefined ) o.init = false;
			if(o.include_recursive == undefined) o.include_recursive=false;
			if(o.reset_regex == undefined) o.reset_regex=false;
			
			$(this).each( function() {
				function showTree(c, treeOptions,focusElementPath) {
					console.log("inside show tree");

					$(c).addClass('wait');

					$(".jqueryFileTree.start").remove();
					var formData = "dir="+escape(treeOptions.dir==undefined?'/':treeOptions.dir)+"&recursive="+(treeOptions.recursive==undefined?false:treeOptions.recursive)+"&files_for_policy="+(treeOptions.files_for_policy==undefined?false:treeOptions.files_for_policy)+"&init="+(treeOptions.init==undefined?false:treeOptions.init)+"&include_recursive="+(treeOptions.include_recursive==undefined?false:treeOptions.include_recursive)+"&reset_regex="+(treeOptions.reset_regex==undefined?false:treeOptions.reset_regex);
					if(treeOptions.include!=undefined && treeOptions.include!=null){
						formData+="&include="+encodeURIComponent(treeOptions.include);
					}
					if(treeOptions.exclude!=undefined && treeOptions.exclude!=null){
						formData+="&exclude="+encodeURIComponent(treeOptions.exclude);
					}
					if(treeOptions.filterType!=undefined && treeOptions.filterType!=null){
            formData+="&filterType="+escape(treeOptions.filterType);
          }
					$.ajax({
					  type: "GET",
					  url: o.script,
					  data: formData,
					contentType: "application/json",
					  success: function(data, status) {
						  console.log("success");
						  hideLoading();
						$(c).find('.start').html('');
						var response = data;
						if(!(response.patch_xml == null)){							
							editPatchWithDataFromServer(response.patch_xml);
						}
						////alert("yes::"+data.generated_tree);
						var tree_content_created=createTreeContent(data.generated_tree);
						console.log("tree_content-created final::"+tree_content_created);
						$(c).removeClass('wait').append(tree_content_created);
						
						
						$("#dirNextButton").prop('disabled', false);
						if( o.root == treeOptions.dir ) $(c).find('UL:hidden').show(); else $(c).find('UL:hidden').slideDown({ duration: o.expandSpeed, easing: o.expandEasing });
							bindTree(c);

							if(focusElementPath){
								console.log("Going to focus on elleemnt by name:"+focusElementPath);

								var elem= $('[name="'+focusElementPath+'"]');
							
								console.log("element:: "+elem);
							
								elem.focus();
							}
						},
						error: function (jqXHR, textStatus, errorThrown){
							hideLoading();
							alert("ERROR");
						},
					});					
				}
				
				var eventHandlerFunction=function() {

					if( $(this).parent().hasClass('directory') ) {
						if( $(this).parent().hasClass('collapsed') ) {
							// Expand
							if( !o.multiFolder ) {
								$(this).parent().parent().find('UL').slideUp({ duration: o.collapseSpeed, easing: o.collapseEasing });
								$(this).parent().parent().find('LI.directory').removeClass('expanded').addClass('collapsed');
							}
							$(this).parent().find('UL').remove(); // cleanup
							var treeOptions = {};
							treeOptions.dir = escape($(this).attr('rel').match( /.*\// ));
							treeOptions.recursive = false;
							treeOptions.files_for_policy = false;
							treeOptions.init=true;

							showTree( $(this).parent(), treeOptions,"" );
							$(this).parent().removeClass('collapsed').addClass('expanded');
						} else {
							// Collapse
							$(this).parent().find('UL').slideUp({ duration: o.collapseSpeed, easing: o.collapseEasing });
							$(this).parent().removeClass('expanded').addClass('collapsed');
						}
					}else if($(this).parent().hasClass('symlink')) {
						console.log("Inside symlink click");
						var targetPath="";
						targetPath=$(this).attr('rel');


						var targetToExpand="";
						targetToExpand=getSymlinkTargetToExpand(targetPath);
						console.log("link targetPath::"+targetPath+" targetToExpand:"+targetToExpand);
						var checkboxElement="checkbox_"+targetToExpand;
						console.log("#################myEle:"+checkboxElement);
						var mainElement=$('[name="'+checkboxElement+'"]').parent();
					if( $('[name="'+targetPath+'"]').length == 0)
					{
					console.log("Ellement do not exist:"+targetPath);
				
											var treeOptions = {};
											treeOptions.dir = targetToExpand;
											treeOptions.recursive = true;
											treeOptions.files_for_policy = false;
				
											showTree( mainElement, treeOptions,targetPath);
											console.log("Before focus..................targetPath:"+targetPath);
											// /$( '[name="'+targetPath+'"]'
											// ).focus();
											// //navigateToLink($(this).attr('value'));
					
					   }else{
							console.log("Element exist:"+targetPath);
							if( mainElement.hasClass('collapsed') ) {
								// Expand
								if( !o.multiFolder ) {
									$(mainElement).parent().parent().find('UL').slideUp({ duration: o.collapseSpeed, easing: o.collapseEasing });
									$(mainElement).parent().parent().find('LI.directory').removeClass('expanded').addClass('collapsed');
								}
								$(mainElement).parent().find('UL').remove(); // cleanup
							
								var treeOptions = {};
								treeOptions.dir = escape($(this).attr('rel').match( /.*\// ));
								treeOptions.recursive = false;
								treeOptions.files_for_policy = false;
								treeOptions.init=true;
		
								showTree( mainElement, treeOptions,targetPath );
								$(this).parent().removeClass('collapsed').addClass('expanded');
							}else{
								$('[name="'+targetPath+'"]').focus();
								//mainElement.focus();	
							} 
							
							
							
					}
						
					}else{
 						
					} 
					return false;
				};
			
			function bindTree(t) {
				$(t).find('LI A').bind(o.folderEvent, eventHandlerFunction);
				$(t).find('LI input').bind(o.folderEvent, function() {

							if( $(this).parent().hasClass('directory') ) {
								if(canPushPatch == false){
									alert("Please wait for the last operation to complete.");
									if(this.checked){
										this.checked = false;
									}else{
										this.checked = true;
									}
								}
								$(this).parent().find('UL').remove(); // cleanup
								var treeOptions = {};
								var checkboxId=escape($(this).attr('id'));
                                if (checkboxId) {
                                    var idArr;

                                    if (checkboxId.indexOf("checkbox_") === 0) {
                                        idArr = checkboxId.valueOf().substring(9, checkboxId.valueOf().length)
                                    }
                                    if (idArr !== null)
                                        treeOptions.dir = idArr;
                                    console.log("############ directory checkbox click: dir" + treeOptions.dir);
                                }

							///	treeOptions.dir = escape($(this).attr('id'));
								treeOptions.recursive = true;
								treeOptions.files_for_policy = false;
								// if($(this).attr('checked')){
								if(this.checked){
									treeOptions.files_for_policy = true;
								}
								treeOptions.init = true;
								showTree( $(this).parent(),  treeOptions,"");
								$(this).parent().removeClass('collapsed').addClass('expanded');

							}else{								
								console.log("**************");
								var rootRegexDir = $(this).attr('rootregexdir');
								console.log("rootRegexDir : "+rootRegexDir );
								if(rootRegexDir == undefined){
									rootRegexDir = "";
								}
								console.log("*rootRegexDir : "+rootRegexDir);
								var checkboxId=escape($(this).attr('id'));
								var elementId="";
								if(checkboxId){

									var idArr=checkboxId.split("_");
									if(idArr.length>1){
										elementId=idArr[1];
									}
								}
								console.log("############ file checkbox click: elementId"+elementId);
								h(elementId, this.checked, rootRegexDir);
							}
							var isChecked = this.checked;// $(this).attr('checked')
															// ;
							if(isChecked){
								$(this).parent().addClass('selected');
							}else{
								$(this).parent().removeClass('selected');
							}
				});
				// Prevent A from triggering the # on non-click events
				if( o.folderEvent.toLowerCase != 'click' ) $(t).find('LI A').bind('click', function() { return false; });
			}
			
			console.log("INIt");

				// Loading message
				if(o.init){
					$(this).html('<ul class="jqueryFileTree start"><li class="wait">' + o.loadMessage + '<li></ul>');
				}
				// Get the initial file list
				o.dir = escape(o.dir);
				console.log("callong show tree");
				showTree( $(this), o ,"");
			});
		}
	});
	
})(jQuery);